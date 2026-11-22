(function (global) {
  'use strict';

  const CAR_SPECS = {
    apex_v8: {
      id: 'apex_v8',
      name: 'Apex V8 GT',
      isEv: false,
      maxSpeedMs: 74.0, // ~266 km/h base
      accelForce: 28.5,
      brakeForce: 46.0,
      turnRate: 2.35,
      grip: 7.4,
      driftGrip: 2.9,
      maxRpm: 8200
    },
    valkyrie_rs: {
      id: 'valkyrie_rs',
      name: 'Valkyrie RS',
      isEv: false,
      maxSpeedMs: 81.0, // ~292 km/h base
      accelForce: 32.5,
      brakeForce: 50.0,
      turnRate: 2.5,
      grip: 8.1,
      driftGrip: 3.1,
      maxRpm: 8800
    },
    spectre_ev: {
      id: 'spectre_ev',
      name: 'Spectre Hyper-EV',
      isEv: true,
      maxSpeedMs: 86.0, // ~310 km/h base
      accelForce: 38.0,
      brakeForce: 54.0,
      turnRate: 2.6,
      grip: 8.4,
      driftGrip: 3.3,
      maxRpm: 11000
    },
    titan_lm: {
      id: 'titan_lm',
      name: 'Titan LM Prototype',
      isEv: false,
      maxSpeedMs: 93.0, // ~335 km/h base
      accelForce: 39.5,
      brakeForce: 58.0,
      turnRate: 2.75,
      grip: 9.2,
      driftGrip: 3.5,
      maxRpm: 9500
    }
  };

  const NUM_CHECKPOINTS = 6;

  function findClosestSampleIndex(samples, pos, hintIdx) {
    const n = samples.length;
    let bestIdx = 0;
    let bestDistSq = Infinity;

    if (hintIdx !== undefined && hintIdx >= 0) {
      const windowRadius = 35;
      for (let offset = -windowRadius; offset <= windowRadius; offset++) {
        const idx = (hintIdx + offset + n) % n;
        const p = samples[idx].point;
        const dx = pos.x - p.x;
        const dz = pos.z - p.z;
        const dSq = dx * dx + dz * dz;
        if (dSq < bestDistSq) {
          bestDistSq = dSq;
          bestIdx = idx;
        }
      }
      if (bestDistSq < 900) {
        return bestIdx;
      }
    }

    for (let i = 0; i < n; i++) {
      const p = samples[i].point;
      const dx = pos.x - p.x;
      const dz = pos.z - p.z;
      const dSq = dx * dx + dz * dz;
      if (dSq < bestDistSq) {
        bestDistSq = dSq;
        bestIdx = i;
      }
    }
    return bestIdx;
  }

  function createRacerState(opts) {
    const baseSpec = CAR_SPECS[opts.carId] || CAR_SPECS.apex_v8;
    const upgrades = opts.upgrades || { engine: 0, turbo: 0, grip: 0, brake: 0 };

    const spec = {
      ...baseSpec,
      maxSpeedMs: baseSpec.maxSpeedMs * (1 + upgrades.engine * 0.045),
      accelForce: baseSpec.accelForce * (1 + upgrades.turbo * 0.065),
      grip: baseSpec.grip * (1 + upgrades.grip * 0.06),
      turnRate: baseSpec.turnRate * (1 + upgrades.grip * 0.04),
      brakeForce: baseSpec.brakeForce * (1 + upgrades.brake * 0.07)
    };

    const { trackData, gridSlot } = opts;
    const n = trackData.numSamples;
    // Place cars on staggered 2x2 grid behind start line
    const backSamples = 4 + gridSlot * 4;
    const spawnIdx = (n - backSamples) % n;
    const samp = trackData.samples[spawnIdx];
    const lateralSlot = gridSlot % 2 === 0 ? -3.0 : 3.0;
    const spawnPos = samp.point.clone().addScaledVector(samp.right, lateralSlot);
    const yaw = Math.atan2(samp.tangent.x, samp.tangent.z);

    return {
      id: opts.id,
      name: opts.name,
      carId: spec.id,
      isPlayer: !!opts.isPlayer,
      isRemote: !!opts.isRemote,
      spec,
      pos: spawnPos,
      yaw,
      vx: 0,
      vz: 0,
      forwardSpeed: 0,
      lateralSpeed: 0,
      steerAngle: 0,
      wheelSpin: 0,
      gear: 1,
      rpm: 1200,
      rpmNorm: 0.14,
      slipAmount: 0,
      isDrifting: false,
      pitchAngle: 0,
      rollAngle: 0,
      splineIdx: spawnIdx,
      prevProgress: samp.u,
      progress: samp.u,
      lap: 1,
      checkpointsVisited: 0,
      finished: false,
      finishTimeMs: 0,
      lapStartMs: 0,
      bestLapMs: 0,
      lastLapMs: 0,
      raceDistance: (samp.u - 1.0) * trackData.totalLength,
      aiLaneOffset: (gridSlot - 1.5) * 1.4,
      aiSkill: opts.aiSkill || 0.92,
      bodyRollVel: 0,
      meshBundle: opts.meshBundle
    };
  }

  function updateRacerPhysics(racer, input, dt, trackData, barrierHalfWidth, totalLaps, raceElapsedMs, canMove, onEvent) {
    const spec = racer.spec;
    const samples = trackData.samples;
    const n = samples.length;

    const throttle = canMove ? Math.max(0, Math.min(1, input.throttle || 0)) : 0;
    const brake = canMove ? Math.max(0, Math.min(1, input.brake || 0)) : 0;
    const steerInput = canMove ? Math.max(-1, Math.min(1, input.steer || 0)) : 0;

    // Forward & Right unit vectors from yaw
    const fwdX = Math.sin(racer.yaw);
    const fwdZ = Math.cos(racer.yaw);
    const rightX = Math.cos(racer.yaw);
    const rightZ = -Math.sin(racer.yaw);

    // Project current world velocity onto car axes
    let vFwd = racer.vx * fwdX + racer.vz * fwdZ;
    let vLat = racer.vx * rightX + racer.vz * rightZ;

    // Steering response scales smoothly with speed
    const speedNorm = Math.min(1, Math.abs(vFwd) / 14.0);
    const highSpeedDamp = 1.0 - Math.min(0.45, (Math.abs(vFwd) / spec.maxSpeedMs) * 0.42);
    const dirSign = vFwd >= -0.5 ? 1 : -1;

    const targetSteer = steerInput * 0.45;
    racer.steerAngle += (targetSteer - racer.steerAngle) * Math.min(1, dt * 14);

    if (Math.abs(vFwd) > 0.3) {
      racer.yaw += steerInput * spec.turnRate * highSpeedDamp * speedNorm * dirSign * dt;
    }

    // Recompute car axes after yaw update
    const nfX = Math.sin(racer.yaw);
    const nfZ = Math.cos(racer.yaw);
    const nrX = Math.cos(racer.yaw);
    const nrZ = -Math.sin(racer.yaw);

    // Longitudinal acceleration / braking / reverse
    if (throttle > 0) {
      const speedRatio = Math.max(0, vFwd / spec.maxSpeedMs);
      const torqueCurve = Math.max(0.48, 1.18 - speedRatio * 0.66);
      vFwd += throttle * spec.accelForce * torqueCurve * dt;
    }

    if (brake > 0) {
      if (vFwd > 1.2) {
        vFwd -= brake * spec.brakeForce * dt;
      } else {
        // Reverse gear
        vFwd -= brake * 18.0 * dt;
        vFwd = Math.max(-22.0, vFwd);
      }
    }

    // Aerodynamic drag & rolling resistance
    const drag = 0.0011 * vFwd * Math.abs(vFwd) + 0.06 * vFwd;
    vFwd -= drag * dt;
    if (throttle === 0 && brake === 0 && Math.abs(vFwd) < 0.25) {
      vFwd = 0;
    }
    vFwd = Math.min(spec.maxSpeedMs, vFwd);

    // Lateral tire grip & drifting physics
    const isHardCorner = Math.abs(steerInput) > 0.65 && Math.abs(vFwd) > 22;
    const activeGrip = (brake > 0.25 && isHardCorner) ? spec.driftGrip : (isHardCorner ? spec.grip * 0.62 : spec.grip);
    vLat *= Math.exp(-activeGrip * dt);

    // Reconstruct world velocity
    racer.vx = nfX * vFwd + nrX * vLat;
    racer.vz = nfZ * vFwd + nrZ * vLat;
    racer.forwardSpeed = vFwd;
    racer.lateralSpeed = vLat;

    // Drift / slip calculation
    const slipRatio = Math.abs(vFwd) > 6 ? Math.min(1.2, Math.abs(vLat) / (Math.abs(vFwd) * 0.36 + 3.0)) : 0;
    racer.slipAmount = slipRatio;
    racer.isDrifting = slipRatio > 0.32 && Math.abs(vFwd) > 16;

    // Integrate position
    racer.pos.x += racer.vx * dt;
    racer.pos.z += racer.vz * dt;

    // Closest spline projection for road elevation & wall barrier collision
    racer.splineIdx = findClosestSampleIndex(samples, racer.pos, racer.splineIdx);
    const samp = samples[racer.splineIdx];
    racer.pos.y = samp.point.y;

    const toCarX = racer.pos.x - samp.point.x;
    const toCarZ = racer.pos.z - samp.point.z;
    const lateralOffset = toCarX * samp.right.x + toCarZ * samp.right.z;

    if (Math.abs(lateralOffset) > barrierHalfWidth) {
      const sign = lateralOffset > 0 ? 1 : -1;
      const excess = Math.abs(lateralOffset) - barrierHalfWidth;
      racer.pos.x -= samp.right.x * sign * excess;
      racer.pos.z -= samp.right.z * sign * excess;

      // Reflect velocity along barrier normal
      const vNormal = racer.vx * samp.right.x + racer.vz * samp.right.z;
      if (vNormal * sign > 0) {
        const impactMag = Math.abs(vNormal);
        racer.vx -= samp.right.x * vNormal * 1.38;
        racer.vz -= samp.right.z * vNormal * 1.38;
        racer.vx *= 0.84;
        racer.vz *= 0.84;

        // Gently align yaw toward track tangent so car never sticks perpendicular to wall
        const tangentYaw = Math.atan2(samp.tangent.x, samp.tangent.z);
        let diff = tangentYaw - racer.yaw;
        while (diff > Math.PI) diff -= Math.PI * 2;
        while (diff < -Math.PI) diff += Math.PI * 2;
        racer.yaw += diff * 0.25;

        if (impactMag > 4.5 && onEvent) {
          onEvent('wall_hit', racer, Math.min(1, impactMag / 26));
        }
      }
    }

    // 6-speed gearbox & RPM computation
    const absKmh = Math.abs(vFwd) * 3.6;
    if (vFwd < -0.8) {
      racer.gear = 0; // R
      racer.rpmNorm = Math.min(0.85, 0.2 + Math.abs(vFwd) / 28);
    } else {
      const topKmh = spec.maxSpeedMs * 3.6;
      const gearBounds = [0, 0.19, 0.34, 0.51, 0.68, 0.84, 1.02];
      const normSpeed = absKmh / topKmh;
      let g = 1;
      for (let i = 1; i <= 6; i++) {
        if (normSpeed >= gearBounds[i - 1]) g = i;
      }
      racer.gear = g;
      const lowB = gearBounds[g - 1] * 0.72;
      const highB = gearBounds[g];
      const inGear = Math.max(0, Math.min(1, (normSpeed - lowB) / Math.max(0.08, highB - lowB)));
      racer.rpmNorm = Math.max(0.14, Math.min(0.99, 0.22 + inGear * 0.75 + (throttle > 0.5 && absKmh < 8 ? 0.25 : 0)));
    }
    racer.rpm = Math.round(900 + racer.rpmNorm * (spec.maxRpm - 900));

    // Wheel spin & suspension pitch/roll
    racer.wheelSpin += (vFwd / 0.36) * dt;
    const targetRoll = -steerInput * Math.min(0.065, Math.abs(vFwd) * 0.0014);
    const targetPitch = (brake > 0.2 ? 0.038 : -throttle * 0.026) * Math.min(1, Math.abs(vFwd) / 15 + 0.3);
    racer.rollAngle += (targetRoll - racer.rollAngle) * Math.min(1, dt * 10);
    racer.pitchAngle += (targetPitch - racer.pitchAngle) * Math.min(1, dt * 10);

    // Checkpoints & Lap Progress
    racer.prevProgress = racer.progress;
    racer.progress = samp.u;

    const sector = Math.min(NUM_CHECKPOINTS - 1, Math.floor(racer.progress * NUM_CHECKPOINTS));
    if (sector === racer.checkpointsVisited + 1) {
      racer.checkpointsVisited = sector;
    }

    // Crossed Start/Finish line forward
    if (racer.prevProgress > 0.84 && racer.progress < 0.16 && racer.checkpointsVisited >= NUM_CHECKPOINTS - 1) {
      const lapDuration = raceElapsedMs - racer.lapStartMs;
      racer.lastLapMs = lapDuration;
      if (racer.bestLapMs === 0 || lapDuration < racer.bestLapMs) {
        racer.bestLapMs = lapDuration;
      }
      racer.lapStartMs = raceElapsedMs;
      racer.checkpointsVisited = 0;
      racer.lap += 1;

      if (racer.lap > totalLaps && !racer.finished) {
        racer.finished = true;
        racer.finishTimeMs = raceElapsedMs;
        if (onEvent) onEvent('finish', racer, lapDuration);
      } else if (onEvent) {
        onEvent('lap', racer, lapDuration);
      }
    }

    const effectiveU = (racer.checkpointsVisited === 0 && racer.progress > 0.8)
      ? racer.progress - 1.0
      : racer.progress;
    racer.raceDistance = (racer.finished ? totalLaps + 1 : (racer.lap - 1) + effectiveU) * trackData.totalLength;

    // Sync 3D mesh transforms
    if (racer.meshBundle) {
      const mb = racer.meshBundle;
      mb.root.position.set(racer.pos.x, racer.pos.y, racer.pos.z);
      mb.root.rotation.y = racer.yaw;
      mb.bodyGroup.rotation.z = racer.rollAngle;
      mb.bodyGroup.rotation.x = racer.pitchAngle;

      mb.wheelPivots.forEach((wp) => {
        if (wp.isFront) {
          wp.pivot.rotation.y = racer.steerAngle * 0.72;
        }
      });
      mb.wheelSpinners.forEach((ws) => {
        ws.rotation.x = racer.wheelSpin;
      });

      mb.taillightMat.emissiveIntensity = brake > 0.1 ? 3.4 : 0.9;
      mb.exhaustFlames.forEach((fm) => {
        fm.opacity = throttle > 0.8 && racer.rpmNorm > 0.82 ? (0.45 + Math.random() * 0.45) : 0;
      });
    }
  }

  function resolveCarCollisions(racers, onEvent) {
    const minDist = 2.55;
    const minDistSq = minDist * minDist;

    for (let i = 0; i < racers.length; i++) {
      for (let j = i + 1; j < racers.length; j++) {
        const a = racers[i];
        const b = racers[j];
        const dx = b.pos.x - a.pos.x;
        const dz = b.pos.z - a.pos.z;
        const dSq = dx * dx + dz * dz;

        if (dSq > 0.0001 && dSq < minDistSq) {
          const dist = Math.sqrt(dSq);
          const nx = dx / dist;
          const nz = dz / dist;
          const overlap = (minDist - dist) * 0.5;

          a.pos.x -= nx * overlap;
          a.pos.z -= nz * overlap;
          b.pos.x += nx * overlap;
          b.pos.z += nz * overlap;

          const rvx = b.vx - a.vx;
          const rvz = b.vz - a.vz;
          const velAlongNormal = rvx * nx + rvz * nz;

          if (velAlongNormal < 0) {
            const impulse = -0.82 * velAlongNormal;
            a.vx -= impulse * nx;
            a.vz -= impulse * nz;
            b.vx += impulse * nx;
            b.vz += impulse * nz;

            if (Math.abs(velAlongNormal) > 3.5 && onEvent) {
              const hitRacer = a.isPlayer ? a : (b.isPlayer ? b : null);
              if (hitRacer) {
                onEvent('car_hit', hitRacer, Math.min(1, Math.abs(velAlongNormal) / 22));
              }
            }
          }
        }
      }
    }
  }

  function computeAiInput(aiRacer, allRacers, playerRacer, trackData) {
    const samples = trackData.samples;
    const n = samples.length;

    // Look ahead 14..22 samples depending on speed
    const lookAhead = Math.round(12 + Math.abs(aiRacer.forwardSpeed) * 0.28);
    const targetIdx = (aiRacer.splineIdx + lookAhead) % n;
    const targetSamp = samples[targetIdx];

    // Obstacle avoidance: steer around slower car directly ahead
    let laneOffset = aiRacer.aiLaneOffset;
    for (let i = 0; i < allRacers.length; i++) {
      const other = allRacers[i];
      if (other === aiRacer) continue;
      const dx = other.pos.x - aiRacer.pos.x;
      const dz = other.pos.z - aiRacer.pos.z;
      const dist = Math.sqrt(dx * dx + dz * dz);
      if (dist < 14 && dist > 0.5) {
        const fwdDot = dx * Math.sin(aiRacer.yaw) + dz * Math.cos(aiRacer.yaw);
        if (fwdDot > 1.0) {
          const rightDot = dx * Math.cos(aiRacer.yaw) - dz * Math.sin(aiRacer.yaw);
          laneOffset += rightDot > 0 ? -3.2 : 3.2;
        }
      }
    }
    laneOffset = Math.max(-6.5, Math.min(6.5, laneOffset));

    const tx = targetSamp.point.x + targetSamp.right.x * laneOffset;
    const tz = targetSamp.point.z + targetSamp.right.z * laneOffset;

    const dx = tx - aiRacer.pos.x;
    const dz = tz - aiRacer.pos.z;
    const desiredYaw = Math.atan2(dx, dz);

    let yawErr = desiredYaw - aiRacer.yaw;
    while (yawErr > Math.PI) yawErr -= Math.PI * 2;
    while (yawErr < -Math.PI) yawErr += Math.PI * 2;

    const steer = Math.max(-1, Math.min(1, yawErr * 2.4));

    // Corner braking lookahead
    const cornerSamp = samples[(aiRacer.splineIdx + 24) % n];
    const curvature = cornerSamp.curvature || 0;

    // Gentle competitive rubber-banding relative to player
    let paceMod = aiRacer.aiSkill;
    if (playerRacer) {
      const gap = aiRacer.raceDistance - playerRacer.raceDistance;
      if (gap > 90) paceMod *= 0.88;
      else if (gap < -90) paceMod *= 1.07;
    }

    const cornerFactor = Math.max(0, curvature - 0.14);
    const desiredSpeed = Math.max(30, aiRacer.spec.maxSpeedMs * paceMod * (1 - Math.min(0.45, cornerFactor * 1.1)));
    let throttle = 1.0;
    let brake = 0.0;

    if (aiRacer.forwardSpeed > desiredSpeed + 4.5) {
      throttle = 0.0;
      brake = 0.72;
    } else if (aiRacer.forwardSpeed > desiredSpeed) {
      throttle = 0.25;
    }

    return { throttle, brake, steer };
  }

  global.RacingPhysics = {
    CAR_SPECS,
    NUM_CHECKPOINTS,
    createRacerState,
    updateRacerPhysics,
    resolveCarCollisions,
    computeAiInput
  };
})(window);
