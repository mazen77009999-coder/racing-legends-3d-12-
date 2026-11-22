(function (global) {
  'use strict';

  const QUALITY_PRESETS = {
    LOW: { maxDpr: 1.0, shadows: false, shadowSize: 512, particles: false, headlights: false, envMap: false },
    MEDIUM: { maxDpr: 1.35, shadows: true, shadowSize: 1024, particles: true, headlights: false, envMap: true },
    HIGH: { maxDpr: 1.75, shadows: true, shadowSize: 2048, particles: true, headlights: false, envMap: true },
    ULTRA: { maxDpr: 2.25, shadows: true, shadowSize: 2048, particles: true, headlights: true, envMap: true }
  };

  class RacingEngineApp {
    constructor() {
      this.canvas = document.getElementById('game-canvas');
      this.renderer = null;
      this.scene = null;
      this.camera = null;
      this.sunLight = null;
      this.ambientLight = null;
      this.carbonTex = null;
      this.envTex = null;

      this.mode = 'IDLE'; // 'RACE' | 'GARAGE' | 'IDLE'
      this.quality = 'HIGH';
      this.cameraMode = 'CHASE'; // 'CHASE' | 'COCKPIT' | 'FRONT'
      this.paused = false;

      // Input state
      this.input = { throttle: 0, brake: 0, steerLeft: 0, steerRight: 0 };
      this.keys = {};

      // Race state
      this.trackData = null;
      this.trackSceneBundle = null;
      this.racers = [];
      this.playerRacer = null;
      this.totalLaps = 3;
      this.countdownTimer = 0;
      this.countdownStep = 5;
      this.raceStarted = false;
      this.raceElapsedMs = 0;
      this.raceFinishedNotified = false;
      this.cameraShake = 0;
      this.camPos = new THREE.Vector3();
      this.camLook = new THREE.Vector3();
      this.headlightSpot = null;
      this.headlightTarget = null;

      // Garage state
      this.garageGroup = null;
      this.garageCar = null;
      this.garageYaw = 0.65;
      this.garagePitch = 0.22;
      this.garageAutoSpin = true;
      this.garagePointerDown = false;
      this.garageLastX = 0;
      this.garageLastY = 0;

      // Particle pool
      this.particles = [];
      this.particleGroup = null;

      // Timing
      this.lastFrameTime = performance.now();
      this.lastTelemetryEmit = 0;
      this.fpsCounter = 60;
      this.fpsFrames = 0;
      this.fpsLastCheck = performance.now();

      this._initRenderer();
      this._bindEvents();
      this._animate = this._animate.bind(this);
      requestAnimationFrame(this._animate);

      this._emitEvent({ type: 'engine_ready' });
    }

    _initRenderer() {
      try {
        this.renderer = new THREE.WebGLRenderer({
          canvas: this.canvas,
          antialias: true,
          powerPreference: 'high-performance'
        });
        this.renderer.outputEncoding = THREE.sRGBEncoding;
        this.renderer.toneMapping = THREE.ACESFilmicToneMapping;
        this.renderer.toneMappingExposure = 1.12;
        this.renderer.shadowMap.enabled = true;
        this.renderer.shadowMap.type = THREE.PCFSoftShadowMap;
        this._applyQualityToRenderer();

        this.scene = new THREE.Scene();
        this.camera = new THREE.PerspectiveCamera(64, window.innerWidth / Math.max(1, window.innerHeight), 0.15, 1400);
        this.carbonTex = global.TextureFactory.createCarbonTexture();
      } catch (e) {
        const errEl = document.getElementById('webgl-error');
        const msgEl = document.getElementById('webgl-error-msg');
        if (errEl) errEl.style.display = 'flex';
        if (msgEl) msgEl.textContent = e.message || 'WebGL initialization failed.';
      }
    }

    _applyQualityToRenderer() {
      if (!this.renderer) return;
      const preset = QUALITY_PRESETS[this.quality] || QUALITY_PRESETS.HIGH;
      const dpr = Math.min(window.devicePixelRatio || 1, preset.maxDpr);
      this.renderer.setPixelRatio(dpr);
      this.renderer.setSize(window.innerWidth, window.innerHeight, false);
      this.renderer.shadowMap.enabled = preset.shadows;
    }

    _clearScene() {
      if (!this.scene) return;
      while (this.scene.children.length > 0) {
        const obj = this.scene.children[0];
        this.scene.remove(obj);
      }
      this.particles = [];
      this.particleGroup = null;
      this.racers = [];
      this.playerRacer = null;
      this.garageGroup = null;
      this.garageCar = null;
      this.headlightSpot = null;
    }

    _bindEvents() {
      window.addEventListener('resize', () => {
        if (!this.renderer || !this.camera) return;
        this.camera.aspect = window.innerWidth / Math.max(1, window.innerHeight);
        this.camera.updateProjectionMatrix();
        this._applyQualityToRenderer();
      });

      window.addEventListener('keydown', (e) => {
        this.keys[e.code] = true;
        global.RacingAudio.init();
      });
      window.addEventListener('keyup', (e) => {
        this.keys[e.code] = false;
      });

      // Touch drag orbit in Garage mode
      this.canvas.addEventListener('pointerdown', (e) => {
        global.RacingAudio.init();
        if (this.mode === 'GARAGE') {
          this.garagePointerDown = true;
          this.garageAutoSpin = false;
          this.garageLastX = e.clientX;
          this.garageLastY = e.clientY;
        }
      });
      window.addEventListener('pointermove', (e) => {
        if (this.mode === 'GARAGE' && this.garagePointerDown) {
          const dx = e.clientX - this.garageLastX;
          const dy = e.clientY - this.garageLastY;
          this.garageYaw += dx * 0.01;
          this.garagePitch = Math.max(0.08, Math.min(0.62, this.garagePitch + dy * 0.005));
          this.garageLastX = e.clientX;
          this.garageLastY = e.clientY;
        }
      });
      window.addEventListener('pointerup', () => {
        this.garagePointerDown = false;
      });
    }

    // --- Public API called from Android Kotlin ---

    setQuality(qualityName) {
      if (!QUALITY_PRESETS[qualityName]) return;
      this.quality = qualityName;
      this._applyQualityToRenderer();
      if (this.sunLight) {
        const preset = QUALITY_PRESETS[this.quality];
        this.sunLight.castShadow = preset.shadows;
      }
    }

    setAudioSettings(enabled, volume) {
      global.RacingAudio.setSettings(enabled, volume);
    }

    setPaused(paused) {
      this.paused = !!paused;
      global.RacingAudio.setPaused(this.paused);
    }

    setCameraMode(mode) {
      if (mode === 'CHASE' || mode === 'COCKPIT' || mode === 'FRONT') {
        this.cameraMode = mode;
      }
    }

    cycleCameraMode() {
      const order = ['CHASE', 'COCKPIT', 'FRONT'];
      const nextIdx = (order.indexOf(this.cameraMode) + 1) % order.length;
      this.cameraMode = order[nextIdx];
      return this.cameraMode;
    }

    setInput(throttle, brake, steerLeft, steerRight) {
      if (throttle > 0 || brake > 0 || steerLeft > 0 || steerRight > 0) {
        global.RacingAudio.init();
      }
      this.input.throttle = throttle;
      this.input.brake = brake;
      this.input.steerLeft = steerLeft;
      this.input.steerRight = steerRight;
    }

    // --- Garage 3D Showroom ---

    startGarage(configJson) {
      const cfg = typeof configJson === 'string' ? JSON.parse(configJson) : configJson;
      if (cfg.quality) this.quality = cfg.quality;
      this._applyQualityToRenderer();
      this._clearScene();
      this.mode = 'GARAGE';
      this.paused = false;
      global.RacingAudio.update(0, 0, 0, false, false);

      this.scene.background = new THREE.Color(0x090d16);
      this.scene.fog = new THREE.FogExp2(0x090d16, 0.025);

      const preset = QUALITY_PRESETS[this.quality] || QUALITY_PRESETS.HIGH;
      if (preset.envMap) {
        this.envTex = global.TextureFactory.createEnvironmentTexture(this.renderer, 'night');
        this.scene.environment = this.envTex;
      } else {
        this.scene.environment = null;
      }

      // Studio lighting
      const ambient = new THREE.AmbientLight(0xdbeafe, 0.75);
      this.scene.add(ambient);

      const keyLight = new THREE.DirectionalLight(0xffffff, 1.9);
      keyLight.position.set(6, 10, 7);
      keyLight.castShadow = preset.shadows;
      keyLight.shadow.mapSize.set(1024, 1024);
      this.scene.add(keyLight);

      const rimCyan = new THREE.PointLight(0x00e5ff, 2.2, 22);
      rimCyan.position.set(-5, 3, -4);
      this.scene.add(rimCyan);

      const rimCrimson = new THREE.PointLight(0xff2a55, 2.0, 22);
      rimCrimson.position.set(5, 2.5, -4);
      this.scene.add(rimCrimson);

      // Showroom dark glossy floor + glowing ring turntable
      const floor = new THREE.Mesh(
        new THREE.CircleGeometry(28, 48),
        new THREE.MeshStandardMaterial({ color: 0x0d121c, roughness: 0.25, metalness: 0.7 })
      );
      floor.rotation.x = -Math.PI / 2;
      floor.receiveShadow = true;
      this.scene.add(floor);

      const turntable = new THREE.Mesh(
        new THREE.CylinderGeometry(3.4, 3.6, 0.14, 48),
        new THREE.MeshStandardMaterial({ color: 0x151b28, roughness: 0.2, metalness: 0.85 })
      );
      turntable.position.y = 0.07;
      turntable.receiveShadow = true;
      this.scene.add(turntable);

      const neonRing = new THREE.Mesh(
        new THREE.RingGeometry(3.38, 3.5, 64),
        new THREE.MeshBasicMaterial({ color: 0x00e5ff, side: THREE.DoubleSide })
      );
      neonRing.rotation.x = -Math.PI / 2;
      neonRing.position.y = 0.145;
      this.scene.add(neonRing);

      this.garageGroup = new THREE.Group();
      this.garageGroup.position.y = 0.14;
      this.scene.add(this.garageGroup);

      this.updateGarageCar(cfg);
      this.garageAutoSpin = true;
    }

    updateGarageCar(configJson) {
      if (this.mode !== 'GARAGE' || !this.garageGroup) return;
      const cfg = typeof configJson === 'string' ? JSON.parse(configJson) : configJson;
      while (this.garageGroup.children.length > 0) {
        this.garageGroup.remove(this.garageGroup.children[0]);
      }
      const preset = QUALITY_PRESETS[this.quality] || QUALITY_PRESETS.HIGH;
      this.garageCar = global.CarBuilder.createCarMesh(cfg, this.carbonTex, preset.shadows);
      this.garageGroup.add(this.garageCar.root);
    }

    // --- Race Mode ---

    startRace(configJson) {
      const cfg = typeof configJson === 'string' ? JSON.parse(configJson) : configJson;
      global.RacingAudio.init();
      if (cfg.quality) this.quality = cfg.quality;
      if (cfg.cameraMode) this.cameraMode = cfg.cameraMode;
      this._applyQualityToRenderer();
      this._clearScene();

      this.mode = 'RACE';
      this.paused = false;
      this.totalLaps = cfg.laps || 3;
      this.countdownTimer = 0;
      this.countdownStep = 5;
      this.raceStarted = false;
      this.raceElapsedMs = 0;
      this.raceFinishedNotified = false;
      this.cameraShake = 0;
      global.RacingAudio.setPaused(false);

      const preset = QUALITY_PRESETS[this.quality] || QUALITY_PRESETS.HIGH;
      this.trackData = global.TrackBuilder.buildTrackData(cfg.trackId || 'silverpeak_gp');
      this.trackSceneBundle = global.TrackBuilder.buildTrackScene(this.scene, this.trackData, this.quality);

      // Environment Reflection Map
      if (preset.envMap) {
        this.envTex = global.TextureFactory.createEnvironmentTexture(this.renderer, this.trackData.def.theme);
        this.scene.environment = this.envTex;
      } else {
        this.scene.environment = null;
      }

      // Dynamic Lighting
      const def = this.trackData.def;
      this.ambientLight = new THREE.AmbientLight(def.ambientColor, def.ambientIntensity);
      this.scene.add(this.ambientLight);

      const hemiLight = new THREE.HemisphereLight(def.skyBottom, def.groundColor, 0.45);
      this.scene.add(hemiLight);

      this.sunLight = new THREE.DirectionalLight(def.sunColor, def.sunIntensity);
      this.sunLight.position.set(def.sunPos[0], def.sunPos[1], def.sunPos[2]);
      this.sunLight.castShadow = preset.shadows;
      this.sunLight.shadow.mapSize.set(preset.shadowSize, preset.shadowSize);
      this.sunLight.shadow.camera.near = 10;
      this.sunLight.shadow.camera.far = 360;
      const d = 95;
      this.sunLight.shadow.camera.left = -d;
      this.sunLight.shadow.camera.right = d;
      this.sunLight.shadow.camera.top = d;
      this.sunLight.shadow.camera.bottom = -d;
      this.sunLight.shadow.bias = -0.0008;
      this.scene.add(this.sunLight);
      this.scene.add(this.sunLight.target);

      // Particle pool for drift smoke & collision sparks
      this.particleGroup = new THREE.Group();
      this.scene.add(this.particleGroup);

      // Build 3 AI (or Multiplayer) Opponents + Player Car on 2x2 grid
      const aiRoster = cfg.opponents || [
        { id: 'ai_1', name: 'K. Rossi', carId: 'valkyrie_rs', paintHex: '#2563EB', rimStyle: 'FORGED_5', spoilerStyle: 'GT_WING', aiSkill: 0.94 },
        { id: 'ai_2', name: 'M. Sato', carId: 'spectre_ev', paintHex: '#10B981', rimStyle: 'TURBINE', spoilerStyle: 'ENDURANCE', aiSkill: 0.91 },
        { id: 'ai_3', name: 'E. Vance', carId: 'apex_v8', paintHex: '#F59E0B', rimStyle: 'SPLIT_MESH', spoilerStyle: 'LIP', aiSkill: 0.88 }
      ];

      aiRoster.slice(0, 3).forEach((opp, idx) => {
        const meshBundle = global.CarBuilder.createCarMesh(opp, this.carbonTex, preset.shadows);
        this.scene.add(meshBundle.root);
        const racer = global.RacingPhysics.createRacerState({
          id: opp.id || `ai_${idx}`,
          name: opp.name || `Driver ${idx + 1}`,
          carId: opp.carId || 'apex_v8',
          isPlayer: false,
          isRemote: !!opp.isRemote,
          gridSlot: idx + 1,
          aiSkill: opp.aiSkill || 0.91,
          trackData: this.trackData,
          meshBundle
        });
        this.racers.push(racer);
      });

      const playerCfg = cfg.player || {
        name: 'You',
        carId: 'apex_v8',
        paintHex: '#E11D48',
        rimStyle: 'FORGED_5',
        spoilerStyle: 'GT_WING',
        upgrades: { engine: 0, turbo: 0, grip: 0, brake: 0 }
      };
      const playerMesh = global.CarBuilder.createCarMesh(playerCfg, this.carbonTex, preset.shadows);
      this.scene.add(playerMesh.root);

      this.playerRacer = global.RacingPhysics.createRacerState({
        id: 'player',
        name: playerCfg.name || 'You',
        carId: playerCfg.carId || 'apex_v8',
        isPlayer: true,
        gridSlot: 0,
        upgrades: playerCfg.upgrades,
        trackData: this.trackData,
        meshBundle: playerMesh
      });
      this.racers.unshift(this.playerRacer);

      // Sync initial meshes on grid
      this.racers.forEach((r) => {
        global.RacingPhysics.updateRacerPhysics(r, { throttle: 0, brake: 0, steer: 0 }, 0.016, this.trackData, this.trackSceneBundle.barrierHalfWidth, this.totalLaps, 0, false, null);
      });

      // Dynamic Headlights Spotlight (for Ultra / Night)
      if (preset.headlights || def.theme === 'night') {
        this.headlightSpot = new THREE.SpotLight(0xdbeafe, 2.8, 72, Math.PI / 5, 0.45, 1.3);
        this.headlightTarget = new THREE.Object3D();
        this.scene.add(this.headlightSpot);
        this.scene.add(this.headlightTarget);
        this.headlightSpot.target = this.headlightTarget;
      }

      // Snap camera behind player car on starting grid
      const p = this.playerRacer;
      this.camPos.set(p.pos.x - Math.sin(p.yaw) * 8.5, p.pos.y + 3.6, p.pos.z - Math.cos(p.yaw) * 8.5);
      this.camLook.set(p.pos.x + Math.sin(p.yaw) * 6, p.pos.y + 1.1, p.pos.z + Math.cos(p.yaw) * 6);
      this.camera.position.copy(this.camPos);
      this.camera.lookAt(this.camLook);

      // Light up first red bulb
      this._updateStartLights(5);
      global.RacingAudio.playCountdownBeep(false);
    }

    _updateStartLights(step) {
      if (!this.trackSceneBundle) return;
      const lights = this.trackSceneBundle.startLights;
      for (let i = 0; i < lights.length; i++) {
        if (step > 0) {
          const litCount = 6 - step;
          const isLit = i < litCount;
          lights[i].emissive.setHex(isLit ? 0xff1122 : 0x000000);
          lights[i].color.setHex(isLit ? 0xff3344 : 0x2a0808);
        } else {
          // Lights out! Green flash briefly then off
          lights[i].emissive.setHex(step === 0 ? 0x00ff7f : 0x000000);
          lights[i].color.setHex(step === 0 ? 0x10b981 : 0x111827);
        }
      }
    }

    _spawnParticles(pos, colorHex, count, speedScale, size) {
      const preset = QUALITY_PRESETS[this.quality] || QUALITY_PRESETS.HIGH;
      if (!preset.particles || !this.particleGroup) return;
      if (this.particles.length > 90) return;

      const geo = new THREE.SphereGeometry(size, 5, 5);
      for (let i = 0; i < count; i++) {
        const mat = new THREE.MeshBasicMaterial({
          color: colorHex,
          transparent: true,
          opacity: 0.78
        });
        const mesh = new THREE.Mesh(geo, mat);
        mesh.position.set(
          pos.x + (Math.random() - 0.5) * 0.8,
          pos.y + 0.22 + Math.random() * 0.3,
          pos.z + (Math.random() - 0.5) * 0.8
        );
        this.particleGroup.add(mesh);
        this.particles.push({
          mesh,
          mat,
          vx: (Math.random() - 0.5) * speedScale,
          vy: 0.6 + Math.random() * speedScale * 0.6,
          vz: (Math.random() - 0.5) * speedScale,
          life: 0.45 + Math.random() * 0.25,
          maxLife: 0.6
        });
      }
    }

    _updateParticles(dt) {
      for (let i = this.particles.length - 1; i >= 0; i--) {
        const pt = this.particles[i];
        pt.life -= dt;
        if (pt.life <= 0) {
          this.particleGroup.remove(pt.mesh);
          pt.mat.dispose();
          this.particles.splice(i, 1);
        } else {
          pt.mesh.position.x += pt.vx * dt;
          pt.mesh.position.y += pt.vy * dt;
          pt.mesh.position.z += pt.vz * dt;
          pt.mesh.scale.addScalar(dt * 1.4);
          pt.mat.opacity = Math.max(0, (pt.life / pt.maxLife) * 0.7);
        }
      }
    }

    _updateCamera(dt) {
      if (!this.playerRacer) return;
      const p = this.playerRacer;
      const fwdX = Math.sin(p.yaw);
      const fwdZ = Math.cos(p.yaw);
      const speedNorm = Math.min(1, Math.abs(p.forwardSpeed) / p.spec.maxSpeedMs);

      if (this.cameraMode === 'COCKPIT') {
        const targetPos = new THREE.Vector3(
          p.pos.x + fwdX * 0.15,
          p.pos.y + 1.12,
          p.pos.z + fwdZ * 0.15
        );
        const lookAt = new THREE.Vector3(
          p.pos.x + fwdX * 26,
          p.pos.y + 0.92,
          p.pos.z + fwdZ * 26
        );
        this.camera.position.copy(targetPos);
        this.camera.lookAt(lookAt);
        this.camera.fov = 68 + speedNorm * 12;
      } else if (this.cameraMode === 'FRONT') {
        const targetPos = new THREE.Vector3(
          p.pos.x + fwdX * 2.25,
          p.pos.y + 0.62,
          p.pos.z + fwdZ * 2.25
        );
        const lookAt = new THREE.Vector3(
          p.pos.x + fwdX * 32,
          p.pos.y + 0.56,
          p.pos.z + fwdZ * 32
        );
        this.camera.position.copy(targetPos);
        this.camera.lookAt(lookAt);
        this.camera.fov = 72 + speedNorm * 14;
      } else {
        // CHASE camera with smooth spring damping & velocity lag
        const distBack = 6.8 + speedNorm * 1.4;
        const height = 2.65 + speedNorm * 0.35;
        const desiredPos = new THREE.Vector3(
          p.pos.x - fwdX * distBack,
          p.pos.y + height,
          p.pos.z - fwdZ * distBack
        );
        const desiredLook = new THREE.Vector3(
          p.pos.x + fwdX * 7.5,
          p.pos.y + 1.05,
          p.pos.z + fwdZ * 7.5
        );
        const smooth = 1 - Math.exp(-10.5 * dt);
        this.camPos.lerp(desiredPos, smooth);
        this.camLook.lerp(desiredLook, 1 - Math.exp(-14 * dt));

        this.camera.position.copy(this.camPos);
        if (this.cameraShake > 0.01) {
          this.camera.position.x += (Math.random() - 0.5) * this.cameraShake;
          this.camera.position.y += (Math.random() - 0.5) * this.cameraShake * 0.7;
          this.cameraShake *= Math.exp(-12 * dt);
        }
        this.camera.lookAt(this.camLook);
        this.camera.fov = 62 + speedNorm * 14;
      }
      this.camera.updateProjectionMatrix();

      // Move sun shadow frustum with player car for crisp shadows everywhere on track
      if (this.sunLight && this.trackData) {
        const def = this.trackData.def;
        this.sunLight.position.set(p.pos.x + def.sunPos[0] * 0.6, p.pos.y + def.sunPos[1] * 0.7, p.pos.z + def.sunPos[2] * 0.6);
        this.sunLight.target.position.copy(p.pos);
      }

      if (this.headlightSpot && this.headlightTarget) {
        this.headlightSpot.position.set(p.pos.x + fwdX * 1.8, p.pos.y + 0.8, p.pos.z + fwdZ * 1.8);
        this.headlightTarget.position.set(p.pos.x + fwdX * 26, p.pos.y + 0.1, p.pos.z + fwdZ * 26);
      }
    }

    _animate(now) {
      requestAnimationFrame(this._animate);
      const rawDt = (now - this.lastFrameTime) / 1000;
      this.lastFrameTime = now;
      const dt = Math.min(0.05, Math.max(0.001, rawDt));

      // FPS tracking
      this.fpsFrames++;
      if (now - this.fpsLastCheck >= 500) {
        this.fpsCounter = Math.round((this.fpsFrames * 1000) / (now - this.fpsLastCheck));
        this.fpsFrames = 0;
        this.fpsLastCheck = now;
      }

      if (!this.renderer || !this.scene || !this.camera) return;

      if (this.mode === 'GARAGE') {
        if (this.garageAutoSpin && !this.garagePointerDown) {
          this.garageYaw += dt * 0.42;
        }
        const radius = 6.2;
        this.camera.position.set(
          Math.sin(this.garageYaw) * Math.cos(this.garagePitch) * radius,
          1.15 + Math.sin(this.garagePitch) * radius * 0.65,
          Math.cos(this.garageYaw) * Math.cos(this.garagePitch) * radius
        );
        this.camera.lookAt(0, 0.72, 0);
        this.camera.fov = 46;
        this.camera.updateProjectionMatrix();
        this.renderer.render(this.scene, this.camera);
        return;
      }

      if (this.mode !== 'RACE' || !this.trackData || !this.playerRacer) {
        return;
      }

      if (!this.paused) {
        // Countdown sequence (5 -> 1 -> 0 = GO)
        if (!this.raceStarted) {
          this.countdownTimer += dt;
          const nextStep = 5 - Math.floor(this.countdownTimer / 0.72);
          if (nextStep !== this.countdownStep) {
            this.countdownStep = Math.max(0, nextStep);
            this._updateStartLights(this.countdownStep);
            if (this.countdownStep === 0) {
              this.raceStarted = true;
              global.RacingAudio.playCountdownBeep(true);
            } else {
              global.RacingAudio.playCountdownBeep(false);
            }
          }
        } else {
          this.raceElapsedMs += dt * 1000;
          if (this.countdownStep === 0 && this.raceElapsedMs > 1200) {
            this.countdownStep = -1;
            this._updateStartLights(-1);
          }
        }

        // Gather player input (Touch + Keyboard fallback)
        const kThrottle = (this.keys['ArrowUp'] || this.keys['KeyW']) ? 1 : 0;
        const kBrake = (this.keys['ArrowDown'] || this.keys['KeyS'] || this.keys['Space']) ? 1 : 0;
        const kLeft = (this.keys['ArrowLeft'] || this.keys['KeyA']) ? 1 : 0;
        const kRight = (this.keys['ArrowRight'] || this.keys['KeyD']) ? 1 : 0;

        const pThrottle = Math.max(this.input.throttle, kThrottle);
        const pBrake = Math.max(this.input.brake, kBrake);
        const pSteer = Math.max(this.input.steerLeft, kLeft) - Math.max(this.input.steerRight, kRight);

        const onPhysicsEvent = (type, racer, val) => {
          if (type === 'wall_hit' || type === 'car_hit') {
            if (racer.isPlayer) {
              this.cameraShake = Math.max(this.cameraShake, 0.38 * val);
              global.RacingAudio.playCollision(val);
              this._spawnParticles(racer.pos, 0xffaa33, 6, 8, 0.14);
            }
          } else if (type === 'lap' && racer.isPlayer) {
            global.RacingAudio.playLapChime(false);
            this._emitEvent({ type: 'lap_complete', lap: racer.lap - 1, lapTimeMs: val });
          } else if (type === 'finish' && racer.isPlayer && !this.raceFinishedNotified) {
            this.raceFinishedNotified = true;
            global.RacingAudio.playLapChime(true);
          }
        };

        // Update player (if finished, autopilot gently coasts)
        const playerInput = this.playerRacer.finished
          ? global.RacingPhysics.computeAiInput(this.playerRacer, this.racers, null, this.trackData)
          : { throttle: pThrottle, brake: pBrake, steer: pSteer };

        global.RacingPhysics.updateRacerPhysics(
          this.playerRacer,
          playerInput,
          dt,
          this.trackData,
          this.trackSceneBundle.barrierHalfWidth,
          this.totalLaps,
          this.raceElapsedMs,
          this.raceStarted,
          onPhysicsEvent
        );

        // Update AI opponents
        for (let i = 1; i < this.racers.length; i++) {
          const ai = this.racers[i];
          const aiInput = global.RacingPhysics.computeAiInput(ai, this.racers, this.playerRacer, this.trackData);
          global.RacingPhysics.updateRacerPhysics(
            ai,
            aiInput,
            dt,
            this.trackData,
            this.trackSceneBundle.barrierHalfWidth,
            this.totalLaps,
            this.raceElapsedMs,
            this.raceStarted,
            onPhysicsEvent
          );
        }

        // Car-to-car collisions
        global.RacingPhysics.resolveCarCollisions(this.racers, onPhysicsEvent);

        // Drift smoke behind player rear wheels
        if (this.playerRacer.isDrifting) {
          const p = this.playerRacer;
          const backPos = p.pos.clone().add(new THREE.Vector3(-Math.sin(p.yaw) * 1.4, 0, -Math.cos(p.yaw) * 1.4));
          this._spawnParticles(backPos, 0xd1d5db, 1, 2.2, 0.24);
        }

        this._updateParticles(dt);
        this._updateCamera(dt);

        // Update Engine & Tire Audio
        global.RacingAudio.update(
          this.raceStarted ? this.playerRacer.rpmNorm : (0.15 + pThrottle * 0.55),
          pThrottle,
          this.playerRacer.slipAmount,
          !this.playerRacer.finished,
          this.playerRacer.spec.isEv
        );
      }

      this.renderer.render(this.scene, this.camera);

      // Emit telemetry to Android Compose HUD (~25Hz)
      if (now - this.lastTelemetryEmit >= 38) {
        this.lastTelemetryEmit = now;
        this._emitTelemetry();
      }
    }

    _emitTelemetry() {
      if (!this.playerRacer) return;
      const sorted = [...this.racers].sort((a, b) => {
        if (a.finished && b.finished) return a.finishTimeMs - b.finishTimeMs;
        if (a.finished) return -1;
        if (b.finished) return 1;
        return b.raceDistance - a.raceDistance;
      });

      const playerPos = sorted.indexOf(this.playerRacer) + 1;
      const p = this.playerRacer;

      const payload = {
        speedKmh: Math.round(Math.abs(p.forwardSpeed) * 3.6),
        rpm: p.rpm,
        rpmNorm: Number(p.rpmNorm.toFixed(3)),
        maxRpm: p.spec.maxRpm,
        gear: p.gear === 0 ? 'R' : String(p.gear),
        lap: Math.min(this.totalLaps, p.lap),
        totalLaps: this.totalLaps,
        position: playerPos,
        totalRacers: this.racers.length,
        lapTimeMs: Math.max(0, Math.round(this.raceElapsedMs - p.lapStartMs)),
        bestLapMs: Math.round(p.bestLapMs),
        elapsedMs: Math.round(this.raceElapsedMs),
        countdownStep: this.countdownStep,
        cameraMode: this.cameraMode,
        isDrifting: p.isDrifting,
        fps: this.fpsCounter,
        finished: p.finished,
        standings: sorted.map((r, idx) => ({
          id: r.id,
          name: r.name,
          carId: r.carId,
          isPlayer: r.isPlayer,
          position: idx + 1,
          bestLapMs: Math.round(r.bestLapMs),
          finishTimeMs: r.finished ? Math.round(r.finishTimeMs) : Math.round(this.raceElapsedMs + (this.totalLaps * this.trackData.totalLength - r.raceDistance) * 16),
          u: Number(r.progress.toFixed(3))
        }))
      };

      if (window.RacingBridge && typeof window.RacingBridge.onTelemetry === 'function') {
        window.RacingBridge.onTelemetry(JSON.stringify(payload));
      }
    }

    _emitEvent(obj) {
      if (window.RacingBridge && typeof window.RacingBridge.onEvent === 'function') {
        window.RacingBridge.onEvent(JSON.stringify(obj));
      }
    }
  }

  global.RacingEngine = new RacingEngineApp();
})(window);
