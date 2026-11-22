(function (global) {
  'use strict';

  class RacingAudio {
    constructor() {
      this.ctx = null;
      this.masterGain = null;
      this.engineOsc1 = null;
      this.engineOsc2 = null;
      this.engineSubOsc = null;
      this.engineFilter = null;
      this.engineGain = null;
      this.screechSource = null;
      this.screechFilter = null;
      this.screechGain = null;
      this.enabled = true;
      this.volume = 0.8;
      this.initialized = false;
      this.mutedByPause = false;
    }

    init() {
      if (this.initialized) {
        if (this.ctx && this.ctx.state === 'suspended') {
          this.ctx.resume().catch(() => {});
        }
        return;
      }
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (!AudioCtx) return;

      try {
        this.ctx = new AudioCtx();
        this.masterGain = this.ctx.createGain();
        this.masterGain.gain.value = this.enabled && !this.mutedByPause ? this.volume : 0;
        this.masterGain.connect(this.ctx.destination);

        // Multi-oscillator sporty engine sound
        this.engineFilter = this.ctx.createBiquadFilter();
        this.engineFilter.type = 'lowpass';
        this.engineFilter.frequency.value = 420;
        this.engineFilter.Q.value = 2.4;

        this.engineGain = this.ctx.createGain();
        this.engineGain.gain.value = 0.0;

        this.engineOsc1 = this.ctx.createOscillator();
        this.engineOsc1.type = 'sawtooth';
        this.engineOsc1.frequency.value = 65;

        this.engineOsc2 = this.ctx.createOscillator();
        this.engineOsc2.type = 'triangle';
        this.engineOsc2.frequency.value = 130;

        this.engineSubOsc = this.ctx.createOscillator();
        this.engineSubOsc.type = 'sine';
        this.engineSubOsc.frequency.value = 32.5;

        const osc2Gain = this.ctx.createGain();
        osc2Gain.gain.value = 0.45;
        const subGain = this.ctx.createGain();
        subGain.gain.value = 0.65;

        this.engineOsc1.connect(this.engineFilter);
        this.engineOsc2.connect(osc2Gain);
        osc2Gain.connect(this.engineFilter);
        this.engineSubOsc.connect(subGain);
        subGain.connect(this.engineFilter);

        this.engineFilter.connect(this.engineGain);
        this.engineGain.connect(this.masterGain);

        this.engineOsc1.start();
        this.engineOsc2.start();
        this.engineSubOsc.start();

        // Noise buffer for tire screech & collisions
        const bufferSize = this.ctx.sampleRate * 2;
        const noiseBuffer = this.ctx.createBuffer(1, bufferSize, this.ctx.sampleRate);
        const data = noiseBuffer.getChannelData(0);
        for (let i = 0; i < bufferSize; i++) {
          data[i] = Math.random() * 2 - 1;
        }
        this.noiseBuffer = noiseBuffer;

        this.screechSource = this.ctx.createBufferSource();
        this.screechSource.buffer = noiseBuffer;
        this.screechSource.loop = true;

        this.screechFilter = this.ctx.createBiquadFilter();
        this.screechFilter.type = 'bandpass';
        this.screechFilter.frequency.value = 1850;
        this.screechFilter.Q.value = 6.0;

        this.screechGain = this.ctx.createGain();
        this.screechGain.gain.value = 0;

        this.screechSource.connect(this.screechFilter);
        this.screechFilter.connect(this.screechGain);
        this.screechGain.connect(this.masterGain);
        this.screechSource.start();

        this.initialized = true;
      } catch (e) {
        // AudioContext might wait for user gesture
      }
    }

    setSettings(enabled, volume) {
      this.enabled = !!enabled;
      this.volume = Math.max(0, Math.min(1, volume || 0.8));
      this._syncMasterGain();
    }

    setPaused(paused) {
      this.mutedByPause = !!paused;
      this._syncMasterGain();
    }

    _syncMasterGain() {
      if (!this.masterGain || !this.ctx) return;
      const target = this.enabled && !this.mutedByPause ? this.volume : 0;
      this.masterGain.gain.setTargetAtTime(target, this.ctx.currentTime, 0.04);
    }

    update(rpmNorm, throttle, slipAmount, isRacing, isEv) {
      if (!this.initialized || !this.ctx) return;
      if (this.ctx.state === 'suspended') return;
      const now = this.ctx.currentTime;

      if (!isRacing) {
        this.engineGain.gain.setTargetAtTime(0, now, 0.08);
        this.screechGain.gain.setTargetAtTime(0, now, 0.08);
        return;
      }

      const clampedRpm = Math.max(0.08, Math.min(1.05, rpmNorm));
      if (isEv) {
        // Futuristic high-pitch electric motor whine
        const freq = 110 + clampedRpm * 520;
        this.engineOsc1.type = 'sine';
        this.engineOsc2.type = 'triangle';
        this.engineOsc1.frequency.setTargetAtTime(freq, now, 0.03);
        this.engineOsc2.frequency.setTargetAtTime(freq * 2.01, now, 0.03);
        this.engineSubOsc.frequency.setTargetAtTime(freq * 0.5, now, 0.03);
        this.engineFilter.frequency.setTargetAtTime(750 + clampedRpm * 1600, now, 0.04);
        this.engineGain.gain.setTargetAtTime(0.07 + throttle * 0.11, now, 0.04);
      } else {
        // Aggressive V8 / GT exhaust growl
        this.engineOsc1.type = 'sawtooth';
        this.engineOsc2.type = 'triangle';
        const baseFreq = 46 + clampedRpm * 165;
        this.engineOsc1.frequency.setTargetAtTime(baseFreq, now, 0.03);
        this.engineOsc2.frequency.setTargetAtTime(baseFreq * 1.98, now, 0.03);
        this.engineSubOsc.frequency.setTargetAtTime(baseFreq * 0.5, now, 0.03);
        const cutoff = 260 + clampedRpm * 1300 + throttle * 450;
        this.engineFilter.frequency.setTargetAtTime(cutoff, now, 0.04);
        const gain = 0.08 + throttle * 0.14 + clampedRpm * 0.05;
        this.engineGain.gain.setTargetAtTime(gain, now, 0.04);
      }

      // Tire screech proportional to drift/slip
      const screechLevel = Math.max(0, Math.min(0.22, (slipAmount - 0.18) * 0.45));
      this.screechGain.gain.setTargetAtTime(screechLevel, now, 0.03);
      this.screechFilter.frequency.setTargetAtTime(1600 + slipAmount * 650, now, 0.05);
    }

    playCollision(intensity) {
      if (!this.initialized || !this.ctx || !this.enabled || this.mutedByPause) return;
      const now = this.ctx.currentTime;
      const mag = Math.max(0.2, Math.min(1.0, intensity));

      // Low impact thump
      const osc = this.ctx.createOscillator();
      const g = this.ctx.createGain();
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(135, now);
      osc.frequency.exponentialRampToValueAtTime(34, now + 0.22);
      g.gain.setValueAtTime(0.45 * mag, now);
      g.gain.exponentialRampToValueAtTime(0.001, now + 0.24);
      osc.connect(g);
      g.connect(this.masterGain);
      osc.start(now);
      osc.stop(now + 0.25);

      // Metallic scrape burst
      if (this.noiseBuffer) {
        const src = this.ctx.createBufferSource();
        src.buffer = this.noiseBuffer;
        const filter = this.ctx.createBiquadFilter();
        filter.type = 'bandpass';
        filter.frequency.value = 900;
        const ng = this.ctx.createGain();
        ng.gain.setValueAtTime(0.28 * mag, now);
        ng.gain.exponentialRampToValueAtTime(0.001, now + 0.18);
        src.connect(filter);
        filter.connect(ng);
        ng.connect(this.masterGain);
        src.start(now);
        src.stop(now + 0.19);
      }
    }

    playCountdownBeep(isGo) {
      if (!this.initialized || !this.ctx || !this.enabled || this.mutedByPause) return;
      const now = this.ctx.currentTime;
      const osc = this.ctx.createOscillator();
      const g = this.ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(isGo ? 880 : 440, now);
      g.gain.setValueAtTime(0.24, now);
      g.gain.exponentialRampToValueAtTime(0.001, now + (isGo ? 0.42 : 0.18));
      osc.connect(g);
      g.connect(this.masterGain);
      osc.start(now);
      osc.stop(now + (isGo ? 0.44 : 0.2));
    }

    playLapChime(isFinish) {
      if (!this.initialized || !this.ctx || !this.enabled || this.mutedByPause) return;
      const now = this.ctx.currentTime;
      const freqs = isFinish ? [523.25, 659.25, 783.99, 1046.5] : [587.33, 880];
      freqs.forEach((f, idx) => {
        const osc = this.ctx.createOscillator();
        const g = this.ctx.createGain();
        osc.type = 'triangle';
        const t0 = now + idx * 0.08;
        osc.frequency.setValueAtTime(f, t0);
        g.gain.setValueAtTime(0.18, t0);
        g.gain.exponentialRampToValueAtTime(0.001, t0 + 0.24);
        osc.connect(g);
        g.connect(this.masterGain);
        osc.start(t0);
        osc.stop(t0 + 0.25);
      });
    }
  }

  global.RacingAudio = new RacingAudio();
})(window);
