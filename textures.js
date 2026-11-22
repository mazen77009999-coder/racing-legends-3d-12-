(function (global) {
  'use strict';

  const TextureFactory = {
    createAsphaltTexture(isNight) {
      const canvas = document.createElement('canvas');
      canvas.width = 512;
      canvas.height = 512;
      const ctx = canvas.getContext('2d');

      ctx.fillStyle = isNight ? '#171B24' : '#252932';
      ctx.fillRect(0, 0, 512, 512);

      // Fine tarmac aggregate noise
      for (let i = 0; i < 18000; i++) {
        const x = Math.random() * 512;
        const y = Math.random() * 512;
        const v = Math.floor(24 + Math.random() * 38);
        ctx.fillStyle = `rgb(${v},${v + 2},${v + 5})`;
        ctx.fillRect(x, y, 2, 2);
      }

      // Rubber racing groove bands
      const groove = ctx.createLinearGradient(0, 0, 512, 0);
      groove.addColorStop(0.0, 'rgba(0,0,0,0)');
      groove.addColorStop(0.28, 'rgba(8,10,14,0.34)');
      groove.addColorStop(0.5, 'rgba(0,0,0,0.08)');
      groove.addColorStop(0.72, 'rgba(8,10,14,0.34)');
      groove.addColorStop(1.0, 'rgba(0,0,0,0)');
      ctx.fillStyle = groove;
      ctx.fillRect(0, 0, 512, 512);

      // White track edge lines
      ctx.fillStyle = isNight ? '#9AB4D6' : '#E2E6ED';
      ctx.fillRect(14, 0, 10, 512);
      ctx.fillRect(488, 0, 10, 512);

      // Dashed center racing guide
      ctx.fillStyle = isNight ? 'rgba(0,229,255,0.35)' : 'rgba(255,255,255,0.28)';
      for (let y = 0; y < 512; y += 64) {
        ctx.fillRect(253, y + 8, 6, 30);
      }

      const tex = new THREE.CanvasTexture(canvas);
      tex.wrapS = THREE.RepeatWrapping;
      tex.wrapT = THREE.RepeatWrapping;
      tex.anisotropy = 8;
      return tex;
    },

    createKerbTexture(primaryHex, secondaryHex) {
      const canvas = document.createElement('canvas');
      canvas.width = 128;
      canvas.height = 512;
      const ctx = canvas.getContext('2d');
      const segs = 8;
      const h = 512 / segs;
      for (let i = 0; i < segs; i++) {
        ctx.fillStyle = i % 2 === 0 ? primaryHex : secondaryHex;
        ctx.fillRect(0, i * h, 128, h);
      }
      // Subtle concrete edge wear
      ctx.fillStyle = 'rgba(0,0,0,0.18)';
      ctx.fillRect(0, 0, 14, 512);
      const tex = new THREE.CanvasTexture(canvas);
      tex.wrapS = THREE.RepeatWrapping;
      tex.wrapT = THREE.RepeatWrapping;
      return tex;
    },

    createCheckeredTexture() {
      const canvas = document.createElement('canvas');
      canvas.width = 256;
      canvas.height = 128;
      const ctx = canvas.getContext('2d');
      const cols = 16;
      const rows = 4;
      const cw = 256 / cols;
      const ch = 128 / rows;
      for (let r = 0; r < rows; r++) {
        for (let c = 0; c < cols; c++) {
          ctx.fillStyle = (r + c) % 2 === 0 ? '#F4F6FA' : '#11141B';
          ctx.fillRect(c * cw, r * ch, cw, ch);
        }
      }
      const tex = new THREE.CanvasTexture(canvas);
      tex.anisotropy = 4;
      return tex;
    },

    createGrandstandTexture(isNight) {
      const canvas = document.createElement('canvas');
      canvas.width = 256;
      canvas.height = 128;
      const ctx = canvas.getContext('2d');
      ctx.fillStyle = isNight ? '#101522' : '#262C38';
      ctx.fillRect(0, 0, 256, 128);
      const palette = isNight
        ? ['#00E5FF', '#FF2A55', '#FFC857', '#7C8BA1', '#E2EAF4']
        : ['#E53935', '#1E88E5', '#FDD835', '#ECEFF1', '#43A047', '#FB8C00'];
      for (let y = 10; y < 120; y += 12) {
        ctx.fillStyle = isNight ? '#1B2234' : '#374151';
        ctx.fillRect(0, y + 8, 256, 3);
        for (let x = 4; x < 252; x += 8) {
          if (Math.random() > 0.16) {
            ctx.fillStyle = palette[Math.floor(Math.random() * palette.length)];
            ctx.fillRect(x, y, 5, 7);
          }
        }
      }
      const tex = new THREE.CanvasTexture(canvas);
      tex.wrapS = THREE.RepeatWrapping;
      tex.wrapT = THREE.ClampToEdgeWrapping;
      return tex;
    },

    createCarbonTexture() {
      const canvas = document.createElement('canvas');
      canvas.width = 64;
      canvas.height = 64;
      const ctx = canvas.getContext('2d');
      ctx.fillStyle = '#12151C';
      ctx.fillRect(0, 0, 64, 64);
      for (let y = 0; y < 64; y += 8) {
        for (let x = 0; x < 64; x += 8) {
          ctx.fillStyle = ((x + y) / 8) % 2 === 0 ? '#1C2029' : '#0D1016';
          ctx.fillRect(x, y, 4, 8);
        }
      }
      const tex = new THREE.CanvasTexture(canvas);
      tex.wrapS = THREE.RepeatWrapping;
      tex.wrapT = THREE.RepeatWrapping;
      tex.repeat.set(6, 6);
      return tex;
    },

    createEnvironmentTexture(renderer, theme) {
      // Generates a crisp procedural PMREM cubemap for realistic car paint & glass reflections
      const pmrem = new THREE.PMREMGenerator(renderer);
      pmrem.compileEquirectangularShader();

      const envScene = new THREE.Scene();
      const skyColor = theme === 'night' ? 0x091124 : theme === 'sunset' ? 0x2a1d3d : 0x5c8dc4;
      const horizonColor = theme === 'night' ? 0x162447 : theme === 'sunset' ? 0xf28b50 : 0xd6e6f5;
      envScene.background = new THREE.Color(skyColor);

      // Luminous horizon ring & overhead studio softboxes for crisp car highlights
      const stripGeo = new THREE.PlaneGeometry(40, 3);
      const stripMat1 = new THREE.MeshBasicMaterial({
        color: theme === 'night' ? 0x00e5ff : 0xffffff,
        side: THREE.DoubleSide
      });
      const stripMat2 = new THREE.MeshBasicMaterial({
        color: theme === 'night' ? 0xff2a55 : horizonColor,
        side: THREE.DoubleSide
      });

      const topStrip1 = new THREE.Mesh(stripGeo, stripMat1);
      topStrip1.position.set(0, 14, -8);
      topStrip1.rotation.x = Math.PI / 2;
      envScene.add(topStrip1);

      const topStrip2 = new THREE.Mesh(stripGeo, stripMat2);
      topStrip2.position.set(0, 14, 8);
      topStrip2.rotation.x = Math.PI / 2;
      envScene.add(topStrip2);

      const horizonRing = new THREE.Mesh(
        new THREE.CylinderGeometry(35, 35, 6, 32, 1, true),
        new THREE.MeshBasicMaterial({ color: horizonColor, side: THREE.BackSide })
      );
      horizonRing.position.y = 2;
      envScene.add(horizonRing);

      const floorReflect = new THREE.Mesh(
        new THREE.CircleGeometry(40, 32),
        new THREE.MeshBasicMaterial({ color: 0x181c24 })
      );
      floorReflect.rotation.x = -Math.PI / 2;
      floorReflect.position.y = -4;
      envScene.add(floorReflect);

      const renderTarget = pmrem.fromScene(envScene, 0.035);
      pmrem.dispose();
      return renderTarget.texture;
    }
  };

  global.TextureFactory = TextureFactory;
})(window);
