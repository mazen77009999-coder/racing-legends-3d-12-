(function (global) {
  'use strict';

  const CarBuilder = {
    createCarMesh(config, carbonTexture, castShadows) {
      const {
        carId = 'apex_v8',
        paintHex = '#E11D48',
        rimStyle = 'FORGED_5',
        spoilerStyle = 'GT_WING'
      } = config || {};

      const root = new THREE.Group();
      const bodyGroup = new THREE.Group();
      root.add(bodyGroup);

      // Materials
      const paintMat = new THREE.MeshPhysicalMaterial({
        color: new THREE.Color(paintHex),
        metalness: 0.62,
        roughness: 0.18,
        clearcoat: 1.0,
        clearcoatRoughness: 0.06,
        envMapIntensity: 1.45
      });

      const carbonMat = new THREE.MeshStandardMaterial({
        color: 0x1e222b,
        map: carbonTexture || null,
        metalness: 0.35,
        roughness: 0.42
      });

      const glassMat = new THREE.MeshPhysicalMaterial({
        color: 0x0b1019,
        metalness: 0.2,
        roughness: 0.06,
        clearcoat: 1.0,
        transparent: true,
        opacity: 0.84,
        envMapIntensity: 1.8
      });

      const accentMat = new THREE.MeshStandardMaterial({
        color: carId === 'spectre_ev' ? 0x00e5ff : 0xd0d7e2,
        metalness: 0.85,
        roughness: 0.2
      });

      const headlightMat = new THREE.MeshStandardMaterial({
        color: 0xe6f7ff,
        emissive: 0x8be9ff,
        emissiveIntensity: 2.2,
        roughness: 0.1
      });

      const taillightMat = new THREE.MeshStandardMaterial({
        color: 0xff1e38,
        emissive: 0xff0022,
        emissiveIntensity: 1.1,
        roughness: 0.15
      });

      // Car archetype proportions
      const isPrototype = carId === 'titan_lm';
      const isEv = carId === 'spectre_ev';
      const isRs = carId === 'valkyrie_rs';

      const bodyLength = isPrototype ? 4.6 : 4.35;
      const bodyWidth = isPrototype ? 2.04 : 1.94;
      const noseDrop = isPrototype ? 0.22 : 0.32;

      // 1. Carbon undertray & front splitter
      const undertray = new THREE.Mesh(
        new THREE.BoxGeometry(bodyWidth + 0.06, 0.1, bodyLength + 0.12),
        carbonMat
      );
      undertray.position.set(0, 0.22, 0);
      bodyGroup.add(undertray);

      const splitter = new THREE.Mesh(
        new THREE.BoxGeometry(bodyWidth + 0.12, 0.06, 0.55),
        carbonMat
      );
      splitter.position.set(0, 0.2, bodyLength * 0.5 - 0.12);
      bodyGroup.add(splitter);

      // 2. Main sculpted lower chassis
      const lowerBody = new THREE.Mesh(
        new THREE.BoxGeometry(bodyWidth, 0.44, bodyLength - 0.15),
        paintMat
      );
      lowerBody.position.set(0, 0.48, 0);
      bodyGroup.add(lowerBody);

      // Aerodynamic sloped front hood
      const hoodGeo = new THREE.BoxGeometry(bodyWidth - 0.16, 0.22, 1.45);
      const hood = new THREE.Mesh(hoodGeo, paintMat);
      hood.position.set(0, 0.62 - (0.34 - noseDrop) * 0.2, 1.18);
      hood.rotation.x = 0.14;
      bodyGroup.add(hood);

      // Sculpted front wheel arches / fenders
      [-1, 1].forEach((side) => {
        const fender = new THREE.Mesh(
          new THREE.BoxGeometry(0.36, 0.34, 1.25),
          paintMat
        );
        fender.position.set(side * (bodyWidth * 0.5 - 0.12), 0.57, 1.22);
        bodyGroup.add(fender);

        const rearHaunch = new THREE.Mesh(
          new THREE.BoxGeometry(0.4, 0.38, 1.45),
          paintMat
        );
        rearHaunch.position.set(side * (bodyWidth * 0.5 - 0.12), 0.62, -1.18);
        bodyGroup.add(rearHaunch);

        // Side intake / skirt accent
        const intake = new THREE.Mesh(
          new THREE.BoxGeometry(0.08, 0.28, 1.1),
          carbonMat
        );
        intake.position.set(side * (bodyWidth * 0.5 + 0.01), 0.46, -0.05);
        bodyGroup.add(intake);

        // Wing mirrors
        const mirror = new THREE.Mesh(
          new THREE.BoxGeometry(0.22, 0.09, 0.14),
          paintMat
        );
        mirror.position.set(side * (bodyWidth * 0.5 + 0.08), 0.85, 0.42);
        bodyGroup.add(mirror);
      });

      // 3. Cabin greenhouse (coupe vs Le Mans bubble canopy)
      const cabinWidth = isPrototype ? 1.05 : 1.38;
      const cabinHeight = isPrototype ? 0.4 : 0.46;
      const cabinLength = isPrototype ? 1.85 : 1.95;

      const cabin = new THREE.Mesh(
        new THREE.BoxGeometry(cabinWidth, cabinHeight, cabinLength),
        glassMat
      );
      cabin.position.set(0, 0.9, -0.15);
      bodyGroup.add(cabin);

      const roof = new THREE.Mesh(
        new THREE.BoxGeometry(cabinWidth - 0.08, 0.07, cabinLength * 0.62),
        isRs || isPrototype ? carbonMat : paintMat
      );
      roof.position.set(0, 0.9 + cabinHeight * 0.5 + 0.01, -0.22);
      bodyGroup.add(roof);

      // Sloped windshield frame
      const windshield = new THREE.Mesh(
        new THREE.BoxGeometry(cabinWidth - 0.04, 0.06, 0.72),
        glassMat
      );
      windshield.position.set(0, 0.9, 0.75);
      windshield.rotation.x = 0.58;
      bodyGroup.add(windshield);

      // Racing stripe for Valkyrie RS / Prototype
      if (isRs || isPrototype) {
        const stripe = new THREE.Mesh(
          new THREE.BoxGeometry(0.28, 0.015, bodyLength - 0.35),
          accentMat
        );
        stripe.position.set(0, 0.72, 0.1);
        bodyGroup.add(stripe);
      }

      // 4. Rear Engine Deck / Shark Fin (Prototype) & Diffuser
      if (isPrototype) {
        const sharkFin = new THREE.Mesh(
          new THREE.BoxGeometry(0.05, 0.38, 1.35),
          paintMat
        );
        sharkFin.position.set(0, 1.02, -1.2);
        bodyGroup.add(sharkFin);
      }

      const rearDiffuser = new THREE.Mesh(
        new THREE.BoxGeometry(bodyWidth - 0.14, 0.22, 0.34),
        carbonMat
      );
      rearDiffuser.position.set(0, 0.32, -bodyLength * 0.5 + 0.06);
      bodyGroup.add(rearDiffuser);

      // Exhaust pipes + flame mesh
      const exhaustFlames = [];
      [-0.36, 0.36].forEach((ex) => {
        const pipe = new THREE.Mesh(
          new THREE.CylinderGeometry(0.065, 0.065, 0.22, 12),
          accentMat
        );
        pipe.rotation.x = Math.PI / 2;
        pipe.position.set(ex, 0.34, -bodyLength * 0.5 - 0.04);
        bodyGroup.add(pipe);

        const flameMat = new THREE.MeshBasicMaterial({
          color: isEv ? 0x00e5ff : 0xff7b00,
          transparent: true,
          opacity: 0.0
        });
        const flame = new THREE.Mesh(
          new THREE.ConeGeometry(0.07, 0.45, 8),
          flameMat
        );
        flame.rotation.x = -Math.PI / 2;
        flame.position.set(ex, 0.34, -bodyLength * 0.5 - 0.28);
        bodyGroup.add(flame);
        exhaustFlames.push(flameMat);
      });

      // 5. Rear Spoiler / Wing (customizable)
      if (spoilerStyle === 'LIP') {
        const lip = new THREE.Mesh(
          new THREE.BoxGeometry(bodyWidth - 0.18, 0.08, 0.2),
          carbonMat
        );
        lip.position.set(0, 0.76, -bodyLength * 0.5 + 0.14);
        lip.rotation.x = -0.2;
        bodyGroup.add(lip);
      } else if (spoilerStyle === 'ENDURANCE') {
        [-0.58, 0.58].forEach((sx) => {
          const strut = new THREE.Mesh(new THREE.BoxGeometry(0.06, 0.42, 0.26), carbonMat);
          strut.position.set(sx, 0.9, -bodyLength * 0.5 + 0.16);
          bodyGroup.add(strut);
        });
        const mainPlane = new THREE.Mesh(
          new THREE.BoxGeometry(bodyWidth + 0.18, 0.06, 0.36),
          carbonMat
        );
        mainPlane.position.set(0, 1.12, -bodyLength * 0.5 + 0.12);
        mainPlane.rotation.x = -0.1;
        bodyGroup.add(mainPlane);
        const upperPlane = new THREE.Mesh(
          new THREE.BoxGeometry(bodyWidth + 0.14, 0.04, 0.22),
          paintMat
        );
        upperPlane.position.set(0, 1.24, -bodyLength * 0.5 + 0.06);
        upperPlane.rotation.x = -0.18;
        bodyGroup.add(upperPlane);
      } else {
        // Default GT_WING
        [-0.52, 0.52].forEach((sx) => {
          const strut = new THREE.Mesh(new THREE.BoxGeometry(0.05, 0.34, 0.2), carbonMat);
          strut.position.set(sx, 0.86, -bodyLength * 0.5 + 0.16);
          bodyGroup.add(strut);
        });
        const wing = new THREE.Mesh(
          new THREE.BoxGeometry(bodyWidth + 0.08, 0.055, 0.32),
          carbonMat
        );
        wing.position.set(0, 1.04, -bodyLength * 0.5 + 0.12);
        wing.rotation.x = -0.08;
        bodyGroup.add(wing);
        [-1, 1].forEach((side) => {
          const endplate = new THREE.Mesh(new THREE.BoxGeometry(0.04, 0.22, 0.38), paintMat);
          endplate.position.set(side * (bodyWidth * 0.5 + 0.05), 1.02, -bodyLength * 0.5 + 0.12);
          bodyGroup.add(endplate);
        });
      }

      // 6. Headlights & Taillight LED bar
      [-0.66, 0.66].forEach((hx) => {
        const hl = new THREE.Mesh(new THREE.BoxGeometry(0.36, 0.11, 0.14), headlightMat);
        hl.position.set(hx, 0.52, bodyLength * 0.5 - 0.08);
        bodyGroup.add(hl);
      });

      const tailBar = new THREE.Mesh(
        new THREE.BoxGeometry(bodyWidth - 0.22, 0.09, 0.08),
        taillightMat
      );
      tailBar.position.set(0, 0.58, -bodyLength * 0.5 + 0.02);
      bodyGroup.add(tailBar);

      // 7. Wheels + Rims + Brake Calipers
      const wheelPositions = [
        { x: -bodyWidth * 0.5 + 0.04, z: 1.32, isFront: true },
        { x: bodyWidth * 0.5 - 0.04, z: 1.32, isFront: true },
        { x: -bodyWidth * 0.5 + 0.04, z: -1.32, isFront: false },
        { x: bodyWidth * 0.5 - 0.04, z: -1.32, isFront: false }
      ];

      const tireMat = new THREE.MeshStandardMaterial({
        color: 0x16181d,
        roughness: 0.78,
        metalness: 0.08
      });

      const rimColor = rimStyle === 'TURBINE' ? 0x1e2430 : rimStyle === 'SPLIT_MESH' ? 0xd4af37 : 0xd8dee9;
      const rimMat = new THREE.MeshStandardMaterial({
        color: rimColor,
        metalness: 0.9,
        roughness: 0.18
      });

      const caliperMat = new THREE.MeshStandardMaterial({
        color: 0xff2a55,
        metalness: 0.5,
        roughness: 0.3
      });

      const wheelPivots = [];
      const wheelSpinners = [];

      wheelPositions.forEach((wp) => {
        const steerPivot = new THREE.Group();
        steerPivot.position.set(wp.x, 0.36, wp.z);
        root.add(steerPivot);

        const spinGroup = new THREE.Group();
        steerPivot.add(spinGroup);

        const tireWidth = wp.isFront ? 0.28 : 0.33;
        const tire = new THREE.Mesh(
          new THREE.CylinderGeometry(0.36, 0.36, tireWidth, 24),
          tireMat
        );
        tire.rotation.z = Math.PI / 2;
        spinGroup.add(tire);

        const rimHub = new THREE.Mesh(
          new THREE.CylinderGeometry(0.24, 0.24, tireWidth + 0.015, 18),
          rimMat
        );
        rimHub.rotation.z = Math.PI / 2;
        spinGroup.add(rimHub);

        const spokeCount = rimStyle === 'SPLIT_MESH' ? 8 : rimStyle === 'TURBINE' ? 10 : 5;
        for (let s = 0; s < spokeCount; s++) {
          const spoke = new THREE.Mesh(
            new THREE.BoxGeometry(tireWidth + 0.025, 0.038, 0.42),
            rimMat
          );
          spoke.rotation.x = (s / spokeCount) * Math.PI;
          spinGroup.add(spoke);
        }

        // Brake caliper (does not spin)
        const caliper = new THREE.Mesh(
          new THREE.BoxGeometry(0.12, 0.18, 0.14),
          caliperMat
        );
        caliper.position.set(wp.x < 0 ? 0.04 : -0.04, 0.08, -0.1);
        steerPivot.add(caliper);

        wheelPivots.push({ pivot: steerPivot, isFront: wp.isFront });
        wheelSpinners.push(spinGroup);
      });

      if (castShadows) {
        root.traverse((obj) => {
          if (obj.isMesh && obj.material !== glassMat) {
            obj.castShadow = true;
            obj.receiveShadow = true;
          }
        });
      }

      return {
        root,
        bodyGroup,
        wheelPivots,
        wheelSpinners,
        taillightMat,
        headlightMat,
        exhaustFlames
      };
    }
  };

  global.CarBuilder = CarBuilder;
})(window);
