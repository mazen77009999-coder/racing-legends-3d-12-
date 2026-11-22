(function (global) {
  'use strict';

  const TRACK_DEFS = {
    silverpeak_gp: {
      id: 'silverpeak_gp',
      name: 'Silverpeak GP Circuit',
      theme: 'sunset',
      halfWidth: 10.5,
      skyTop: 0x1b1536,
      skyBottom: 0xe67e45,
      fogColor: 0x3b2844,
      ambientColor: 0xffd1b3,
      ambientIntensity: 0.72,
      sunColor: 0xffa96b,
      sunIntensity: 1.85,
      sunPos: [140, 85, -110],
      groundColor: 0x263424,
      kerbPrimary: '#DC2626',
      kerbSecondary: '#F3F4F6',
      barrierColor: 0xd0d5dd,
      barrierAccent: 0xe11d48,
      controlPoints: [
        [0, 0, -140],
        [0, 0, 20],
        [15, 0, 150],
        [95, 2, 220],
        [205, 4, 185],
        [240, 2, 85],
        [175, 0, 15],
        [210, -2, -85],
        [265, 0, -185],
        [185, 3, -270],
        [75, 1, -245],
        [12, 0, -205]
      ]
    },
    neo_tokyo: {
      id: 'neo_tokyo',
      name: 'Neo Tokyo Midnight',
      theme: 'night',
      halfWidth: 10.0,
      skyTop: 0x050914,
      skyBottom: 0x122244,
      fogColor: 0x091124,
      ambientColor: 0x5c85d6,
      ambientIntensity: 0.68,
      sunColor: 0x99ccff,
      sunIntensity: 1.25,
      sunPos: [-90, 130, 80],
      groundColor: 0x0c101a,
      kerbPrimary: '#00E5FF',
      kerbSecondary: '#111827',
      barrierColor: 0x1f2937,
      barrierAccent: 0x00e5ff,
      controlPoints: [
        [0, 0, -150],
        [0, 0, 40],
        [35, 0, 165],
        [145, 0, 195],
        [235, 2, 115],
        [190, 0, 15],
        [115, 0, -20],
        [180, 0, -125],
        [235, 0, -225],
        [125, 0, -280],
        [25, 0, -240]
      ]
    },
    alpine_pass: {
      id: 'alpine_pass',
      name: 'Mont Blanc Alpine Pass',
      theme: 'day',
      halfWidth: 9.8,
      skyTop: 0x2e6eb5,
      skyBottom: 0xbfe0f7,
      fogColor: 0x9ec4e2,
      ambientColor: 0xe6f2ff,
      ambientIntensity: 0.78,
      sunColor: 0xfff3d6,
      sunIntensity: 1.95,
      sunPos: [120, 140, 100],
      groundColor: 0x2e4a2b,
      kerbPrimary: '#EA580C',
      kerbSecondary: '#F8FAFC',
      barrierColor: 0x94a3b8,
      barrierAccent: 0xf97316,
      controlPoints: [
        [0, 0, -135],
        [0, 2, 35],
        [-55, 6, 155],
        [25, 11, 250],
        [145, 9, 225],
        [220, 4, 120],
        [165, 1, 10],
        [240, 5, -105],
        [210, 8, -235],
        [95, 4, -270],
        [10, 1, -215]
      ]
    }
  };

  function buildTrackData(trackId) {
    const def = TRACK_DEFS[trackId] || TRACK_DEFS.silverpeak_gp;
    const vecPoints = def.controlPoints.map((p) => new THREE.Vector3(p[0], p[1], p[2]));
    const curve = new THREE.CatmullRomCurve3(vecPoints, true, 'catmullrom', 0.45);
    const totalLength = curve.getLength();
    const numSamples = 600;
    const samples = [];

    for (let i = 0; i < numSamples; i++) {
      const u = i / numSamples;
      const point = curve.getPointAt(u);
      const tangent = curve.getTangentAt(u).normalize();
      const up = new THREE.Vector3(0, 1, 0);
      const right = new THREE.Vector3().crossVectors(tangent, up).normalize();
      samples.push({
        index: i,
        u,
        s: u * totalLength,
        point,
        tangent,
        right
      });
    }

    // Compute curvature estimate for AI speed planning
    for (let i = 0; i < numSamples; i++) {
      const prev = samples[(i - 4 + numSamples) % numSamples];
      const next = samples[(i + 4) % numSamples];
      const dot = Math.max(-1, Math.min(1, prev.tangent.dot(next.tangent)));
      samples[i].curvature = Math.acos(dot);
    }

    return {
      def,
      curve,
      totalLength,
      numSamples,
      samples
    };
  }

  function createRibbonGeometry(samples, innerOffset, outerOffset, yOffset, uRepeatLength) {
    const n = samples.length;
    const positions = [];
    const normals = [];
    const uvs = [];
    const indices = [];

    for (let i = 0; i <= n; i++) {
      const samp = samples[i % n];
      const vCoord = (i / n) * uRepeatLength;

      const pIn = samp.point.clone().addScaledVector(samp.right, innerOffset);
      pIn.y += yOffset;
      const pOut = samp.point.clone().addScaledVector(samp.right, outerOffset);
      pOut.y += yOffset;

      positions.push(pIn.x, pIn.y, pIn.z);
      positions.push(pOut.x, pOut.y, pOut.z);

      normals.push(0, 1, 0, 0, 1, 0);
      uvs.push(0, vCoord, 1, vCoord);
    }

    for (let i = 0; i < n; i++) {
      const a = i * 2;
      const b = a + 1;
      const c = a + 2;
      const d = a + 3;
      indices.push(a, c, b);
      indices.push(b, c, d);
    }

    const geo = new THREE.BufferGeometry();
    geo.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3));
    geo.setAttribute('normal', new THREE.Float32BufferAttribute(normals, 3));
    geo.setAttribute('uv', new THREE.Float32BufferAttribute(uvs, 2));
    geo.setIndex(indices);
    geo.computeVertexNormals();
    return geo;
  }

  function createBarrierWallGeometry(samples, sideOffset, height, thickness) {
    const n = samples.length;
    const positions = [];
    const indices = [];
    const uvs = [];

    const sign = sideOffset >= 0 ? 1 : -1;
    const innerOff = sideOffset;
    const outerOff = sideOffset + sign * thickness;

    for (let i = 0; i <= n; i++) {
      const samp = samples[i % n];
      const vCoord = i * 0.25;
      const p0 = samp.point.clone().addScaledVector(samp.right, innerOff);
      const p1 = samp.point.clone().addScaledVector(samp.right, innerOff);
      p1.y += height;
      const p2 = samp.point.clone().addScaledVector(samp.right, outerOff);
      p2.y += height;
      const p3 = samp.point.clone().addScaledVector(samp.right, outerOff);

      positions.push(p0.x, p0.y, p0.z);
      positions.push(p1.x, p1.y, p1.z);
      positions.push(p2.x, p2.y, p2.z);
      positions.push(p3.x, p3.y, p3.z);

      uvs.push(0, vCoord, 0.33, vCoord, 0.66, vCoord, 1, vCoord);
    }

    for (let i = 0; i < n; i++) {
      const base = i * 4;
      const next = (i + 1) * 4;
      // Inner face (0-1)
      indices.push(base + 0, next + 0, base + 1);
      indices.push(base + 1, next + 0, next + 1);
      // Top face (1-2)
      indices.push(base + 1, next + 1, base + 2);
      indices.push(base + 2, next + 1, next + 2);
      // Outer face (2-3)
      indices.push(base + 2, next + 2, base + 3);
      indices.push(base + 3, next + 2, next + 3);
    }

    const geo = new THREE.BufferGeometry();
    geo.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3));
    geo.setAttribute('uv', new THREE.Float32BufferAttribute(uvs, 2));
    geo.setIndex(indices);
    geo.computeVertexNormals();
    return geo;
  }

  function buildTrackScene(scene, trackData, qualityPreset) {
    const { def, samples, totalLength } = trackData;
    const isNight = def.theme === 'night';
    const trackGroup = new THREE.Group();
    scene.add(trackGroup);

    // Sky & Fog
    scene.background = new THREE.Color(def.skyBottom);
    scene.fog = new THREE.FogExp2(def.fogColor, isNight ? 0.0021 : 0.0014);

    // Sky dome gradient
    const skyCanvas = document.createElement('canvas');
    skyCanvas.width = 4;
    skyCanvas.height = 256;
    const sctx = skyCanvas.getContext('2d');
    const skyGrad = sctx.createLinearGradient(0, 0, 0, 256);
    skyGrad.addColorStop(0.0, '#' + def.skyTop.toString(16).padStart(6, '0'));
    skyGrad.addColorStop(0.55, '#' + def.skyBottom.toString(16).padStart(6, '0'));
    skyGrad.addColorStop(1.0, '#' + def.groundColor.toString(16).padStart(6, '0'));
    sctx.fillStyle = skyGrad;
    sctx.fillRect(0, 0, 4, 256);
    const skyTex = new THREE.CanvasTexture(skyCanvas);
    const skyDome = new THREE.Mesh(
      new THREE.SphereGeometry(900, 24, 16),
      new THREE.MeshBasicMaterial({ map: skyTex, side: THREE.BackSide, fog: false })
    );
    trackGroup.add(skyDome);

    // Ground plane
    const groundMat = new THREE.MeshStandardMaterial({
      color: def.groundColor,
      roughness: 0.94,
      metalness: 0.04
    });
    const ground = new THREE.Mesh(new THREE.PlaneGeometry(1600, 1600), groundMat);
    ground.rotation.x = -Math.PI / 2;
    ground.position.y = -0.35;
    ground.receiveShadow = true;
    trackGroup.add(ground);

    // 1. Asphalt Surface
    const asphaltTex = global.TextureFactory.createAsphaltTexture(isNight);
    const roadGeo = createRibbonGeometry(
      samples,
      -def.halfWidth,
      def.halfWidth,
      0.04,
      Math.round(totalLength / 18)
    );
    const roadMat = new THREE.MeshStandardMaterial({
      map: asphaltTex,
      roughness: isNight ? 0.34 : 0.68,
      metalness: isNight ? 0.28 : 0.08,
      envMapIntensity: isNight ? 1.15 : 0.5
    });
    const roadMesh = new THREE.Mesh(roadGeo, roadMat);
    roadMesh.receiveShadow = true;
    trackGroup.add(roadMesh);

    // 2. FIA Kerbs on both sides
    const kerbTex = global.TextureFactory.createKerbTexture(def.kerbPrimary, def.kerbSecondary);
    const kerbMat = new THREE.MeshStandardMaterial({
      map: kerbTex,
      roughness: 0.6,
      metalness: 0.05
    });
    const kerbWidth = 1.25;
    const leftKerb = new THREE.Mesh(
      createRibbonGeometry(samples, -def.halfWidth - kerbWidth, -def.halfWidth, 0.06, Math.round(totalLength / 6)),
      kerbMat
    );
    const rightKerb = new THREE.Mesh(
      createRibbonGeometry(samples, def.halfWidth, def.halfWidth + kerbWidth, 0.06, Math.round(totalLength / 6)),
      kerbMat
    );
    leftKerb.receiveShadow = true;
    rightKerb.receiveShadow = true;
    trackGroup.add(leftKerb, rightKerb);

    // 3. Runoff / Gravel apron
    const apronMat = new THREE.MeshStandardMaterial({
      color: isNight ? 0x111827 : 0x3f434a,
      roughness: 0.9
    });
    const apronOffset = def.halfWidth + kerbWidth + 2.8;
    const leftApron = new THREE.Mesh(
      createRibbonGeometry(samples, -apronOffset, -def.halfWidth - kerbWidth, 0.01, 10),
      apronMat
    );
    const rightApron = new THREE.Mesh(
      createRibbonGeometry(samples, def.halfWidth + kerbWidth, apronOffset, 0.01, 10),
      apronMat
    );
    trackGroup.add(leftApron, rightApron);

    // 4. Safety Barriers
    const barrierOffset = def.halfWidth + kerbWidth + 2.2;
    const barrierMat = new THREE.MeshStandardMaterial({
      color: def.barrierColor,
      roughness: 0.48,
      metalness: 0.35
    });
    const leftBarrier = new THREE.Mesh(
      createBarrierWallGeometry(samples, -barrierOffset, 1.35, 0.6),
      barrierMat
    );
    const rightBarrier = new THREE.Mesh(
      createBarrierWallGeometry(samples, barrierOffset, 1.35, 0.6),
      barrierMat
    );
    if (qualityPreset !== 'LOW') {
      leftBarrier.castShadow = true;
      rightBarrier.castShadow = true;
    }
    trackGroup.add(leftBarrier, rightBarrier);

    // Glowing barrier accent strip in Night City
    if (isNight) {
      const neonStripMat = new THREE.MeshBasicMaterial({ color: def.barrierAccent });
      const leftNeon = new THREE.Mesh(
        createRibbonGeometry(samples, -barrierOffset + 0.05, -barrierOffset + 0.2, 1.38, 1),
        neonStripMat
      );
      const rightNeon = new THREE.Mesh(
        createRibbonGeometry(samples, barrierOffset - 0.2, barrierOffset - 0.05, 1.38, 1),
        neonStripMat
      );
      trackGroup.add(leftNeon, rightNeon);
    }

    // 5. Start/Finish Checkered Line & Overhead Gantry with 5 Starting Lights
    const s0 = samples[0];
    const angle0 = Math.atan2(s0.tangent.x, s0.tangent.z);
    const finishGroup = new THREE.Group();
    finishGroup.position.copy(s0.point);
    finishGroup.rotation.y = angle0;

    const checkerTex = global.TextureFactory.createCheckeredTexture();
    const finishLine = new THREE.Mesh(
      new THREE.PlaneGeometry(def.halfWidth * 2, 4.2),
      new THREE.MeshStandardMaterial({ map: checkerTex, roughness: 0.55 })
    );
    finishLine.rotation.x = -Math.PI / 2;
    finishLine.position.y = 0.065;
    finishLine.receiveShadow = true;
    finishGroup.add(finishLine);

    // Gantry pillars & bridge
    const gantryMat = new THREE.MeshStandardMaterial({
      color: 0x161b26,
      metalness: 0.75,
      roughness: 0.3
    });
    [-def.halfWidth - 1.5, def.halfWidth + 1.5].forEach((px) => {
      const pillar = new THREE.Mesh(new THREE.BoxGeometry(1.1, 8.5, 1.4), gantryMat);
      pillar.position.set(px, 4.25, 0);
      pillar.castShadow = qualityPreset !== 'LOW';
      finishGroup.add(pillar);
    });
    const bridge = new THREE.Mesh(
      new THREE.BoxGeometry(def.halfWidth * 2 + 4.4, 1.9, 1.6),
      gantryMat
    );
    bridge.position.set(0, 7.6, 0);
    finishGroup.add(bridge);

    // 5 F1-style Starting Lights
    const startLights = [];
    for (let i = 0; i < 5; i++) {
      const housing = new THREE.Mesh(
        new THREE.BoxGeometry(0.95, 1.4, 0.5),
        new THREE.MeshStandardMaterial({ color: 0x0b0d12, roughness: 0.4 })
      );
      const lx = (i - 2) * 1.45;
      housing.position.set(lx, 6.2, -0.95);
      finishGroup.add(housing);

      const bulbMat = new THREE.MeshStandardMaterial({
        color: 0x2a0808,
        emissive: 0x000000,
        emissiveIntensity: 2.2
      });
      const bulb = new THREE.Mesh(new THREE.SphereGeometry(0.3, 12, 12), bulbMat);
      bulb.position.set(lx, 6.1, -1.22);
      finishGroup.add(bulb);
      startLights.push(bulbMat);
    }
    trackGroup.add(finishGroup);

    // 6. Grandstands & Pit Complex along main straight & key corners
    const grandstandTex = global.TextureFactory.createGrandstandTexture(isNight);
    const standMat = new THREE.MeshStandardMaterial({
      map: grandstandTex,
      roughness: 0.75
    });
    const structMat = new THREE.MeshStandardMaterial({
      color: isNight ? 0x1e2536 : 0x4b5563,
      metalness: 0.4,
      roughness: 0.6
    });

    const standIndices = [12, 32, 140, 275, 430, 560];
    standIndices.forEach((idx, si) => {
      const samp = samples[idx % samples.length];
      const side = si % 2 === 0 ? -1 : 1;
      const dist = barrierOffset + 10.5;
      const standGroup = new THREE.Group();
      const pos = samp.point.clone().addScaledVector(samp.right, side * dist);
      standGroup.position.copy(pos);
      standGroup.rotation.y = Math.atan2(samp.tangent.x, samp.tangent.z) + (side > 0 ? Math.PI : 0);

      const base = new THREE.Mesh(new THREE.BoxGeometry(8, 7.5, 36), structMat);
      base.position.set(-2.5, 3.75, 0);
      standGroup.add(base);

      const tiers = new THREE.Mesh(new THREE.BoxGeometry(6.5, 6.5, 35), standMat);
      tiers.position.set(1.2, 3.6, 0);
      tiers.rotation.z = 0.35;
      standGroup.add(tiers);

      const canopy = new THREE.Mesh(new THREE.BoxGeometry(10.5, 0.45, 37), structMat);
      canopy.position.set(-0.5, 8.8, 0);
      canopy.rotation.z = -0.14;
      standGroup.add(canopy);

      trackGroup.add(standGroup);
    });

    // 7. Trackside Floodlight Towers / Scenery Props
    const propStep = qualityPreset === 'LOW' ? 30 : 18;
    const poleMat = new THREE.MeshStandardMaterial({ color: 0x374151, metalness: 0.6 });
    const lampMat = new THREE.MeshBasicMaterial({ color: isNight ? 0x93f5ff : 0xfff5d6 });

    for (let i = 0; i < samples.length; i += propStep) {
      const samp = samples[i];
      if (i % (propStep * 2) === 0) {
        const side = (i / propStep) % 4 === 0 ? 1 : -1;
        const pos = samp.point.clone().addScaledVector(samp.right, side * (barrierOffset + 2.2));
        const pole = new THREE.Mesh(new THREE.CylinderGeometry(0.18, 0.24, 11, 6), poleMat);
        pole.position.set(pos.x, pos.y + 5.5, pos.z);
        trackGroup.add(pole);

        const head = new THREE.Mesh(new THREE.BoxGeometry(1.8, 0.5, 0.8), lampMat);
        head.position.set(pos.x, pos.y + 11.1, pos.z);
        head.lookAt(samp.point.x, samp.point.y + 1, samp.point.z);
        trackGroup.add(head);
      }
    }

    // Track theme specific surrounding architecture / nature
    if (isNight) {
      const bldgMat = new THREE.MeshStandardMaterial({
        color: 0x0f172a,
        roughness: 0.35,
        metalness: 0.65
      });
      const winColors = [0x00e5ff, 0xff2a55, 0xfbbf24];
      for (let i = 0; i < 42; i++) {
        const ang = (i / 42) * Math.PI * 2;
        const rad = 260 + (i % 5) * 45;
        const bx = 110 + Math.cos(ang) * rad;
        const bz = -40 + Math.sin(ang) * rad;
        const h = 38 + ((i * 37) % 85);
        const w = 18 + ((i * 13) % 20);
        const tower = new THREE.Mesh(new THREE.BoxGeometry(w, h, w), bldgMat);
        tower.position.set(bx, h * 0.5 - 1, bz);
        trackGroup.add(tower);

        const neonBand = new THREE.Mesh(
          new THREE.BoxGeometry(w + 0.4, 1.2, w + 0.4),
          new THREE.MeshBasicMaterial({ color: winColors[i % winColors.length] })
        );
        neonBand.position.set(bx, h - 3, bz);
        trackGroup.add(neonBand);
      }
    } else if (def.theme === 'day') {
      // Alpine pine trees & distant peaks
      const trunkMat = new THREE.MeshStandardMaterial({ color: 0x4b3621, roughness: 0.9 });
      const leavesMat = new THREE.MeshStandardMaterial({ color: 0x1e4620, roughness: 0.85 });
      for (let i = 0; i < samples.length; i += 14) {
        const samp = samples[i];
        const side = i % 28 === 0 ? 1 : -1;
        const dist = barrierOffset + 18 + (i % 19);
        const pos = samp.point.clone().addScaledVector(samp.right, side * dist);
        const trunk = new THREE.Mesh(new THREE.CylinderGeometry(0.35, 0.5, 3, 6), trunkMat);
        trunk.position.set(pos.x, pos.y + 1.5, pos.z);
        const cone = new THREE.Mesh(new THREE.ConeGeometry(2.6, 8.5, 7), leavesMat);
        cone.position.set(pos.x, pos.y + 6.5, pos.z);
        trackGroup.add(trunk, cone);
      }
      const peakMat = new THREE.MeshStandardMaterial({ color: 0x64748b, roughness: 0.9 });
      for (let i = 0; i < 16; i++) {
        const ang = (i / 16) * Math.PI * 2;
        const px = 110 + Math.cos(ang) * 420;
        const pz = -20 + Math.sin(ang) * 420;
        const ph = 90 + (i % 4) * 35;
        const peak = new THREE.Mesh(new THREE.ConeGeometry(75, ph, 6), peakMat);
        peak.position.set(px, ph * 0.45, pz);
        trackGroup.add(peak);
      }
    }

    return {
      trackGroup,
      startLights,
      barrierHalfWidth: def.halfWidth + 1.15
    };
  }

  global.TrackBuilder = {
    TRACK_DEFS,
    buildTrackData,
    buildTrackScene
  };
})(window);
