const toggle = document.querySelector('.nav__toggle');
const links = document.querySelector('.nav__links');
const toast = document.querySelector('.toast');
const toastMessage = document.querySelector('.toast__message');
const status = document.getElementById('status');
const chips = document.querySelectorAll('.chip');
const controlModes = document.querySelectorAll('.control__mode');
const controlModeStatus = document.getElementById('control-mode-status');
const controlUrlInput = document.getElementById('control-url');
const controlUrlResult = document.getElementById('control-url-result');
const controlUrlCheck = document.getElementById('control-check');
const controlScan = document.getElementById('control-scan');
const controlStatusList = document.getElementById('control-status');

const statuses = [
  'Ready to send Gold alert',
  'Emergency video recording to secure cloud',
  'Smart sensors monitoring falls',
  'Complete digital shield active',
  'Gold breach detection in progress',
];

let statusIndex = 0;
let toastTimeout;
let controlStatusTimeout;
let activeControlMode = 'block';

const controlStatusItems = controlStatusList ? Array.from(controlStatusList.querySelectorAll('li')) : [];

function updateStatus() {
  statusIndex = (statusIndex + 1) % statuses.length;
  if (status) {
    status.textContent = statuses[statusIndex];
  }
}

function showToast(message) {
  if (window.Android && typeof window.Android.showToast === 'function') {
    window.Android.showToast(message);
  } else {
    if (!toast || !toastMessage) return;
    toastMessage.textContent = message;
    toast.hidden = false;
    toast.setAttribute('data-visible', 'true');

    clearTimeout(toastTimeout);
    toastTimeout = setTimeout(() => {
      toast.removeAttribute('data-visible');
      setTimeout(() => {
        toast.hidden = true;
      }, 400);
    }, 2200);
  }
}

chips.forEach((chip) => {
  chip.addEventListener('click', () => {
    const moduleName = chip.dataset.module;
    if (moduleName === 'Royal S.O.S. button') {
      if (window.Android && typeof window.Android.triggerSos === 'function') {
        window.Android.triggerSos();
      } else {
        showToast('SOS feature not available in web preview');
      }
    } else {
      showToast(`Running ${moduleName} sequence`);
    }
  });
});

function setControlMode(target, silent = false) {
  if (!target) return;
  activeControlMode = target.dataset.mode || 'block';
  controlModes.forEach((button) => {
    const isActive = button === target;
    button.setAttribute('aria-pressed', String(isActive));
  });
  if (controlModeStatus) {
    controlModeStatus.textContent =
      activeControlMode === 'report' ? 'Reporting mode ready.' : 'Blocking mode armed.';
  }
  if (!silent) {
    showToast(`Mode set to ${activeControlMode}`);
  }
}

controlModes.forEach((button) => {
  button.addEventListener('click', () => {
    setControlMode(button);
  });
});

function evaluateUrl(rawUrl) {
  if (!rawUrl) {
    return {
      tone: 'neutral',
      message: 'Enter a URL to analyze.',
    };
  }

  try {
    const url = new URL(rawUrl);
    const host = url.hostname;
    const looksIP = /^\d{1,3}(\.\d{1,3}){3}$/.test(host);
    const hasUnicode = host.normalize() !== host || /[^\x00-\x7F]/.test(host);
    const longPath = (url.pathname + url.search).length > 120;
    const httpOnly = url.protocol === 'http:';
    const suspiciousTlds = ['zip', 'mov', 'gq', 'tk', 'ml', 'cf'];
    const badTld = suspiciousTlds.some((tld) => host.endsWith(`.${tld}`));

    const signals = [
      looksIP && 'IP literal domain',
      hasUnicode && 'Unicode or punycode',
      longPath && 'Very long path/query',
      httpOnly && 'No HTTPS',
      badTld && 'Suspicious TLD',
    ].filter(Boolean);

    const score = (looksIP ? 2 : 0) + (hasUnicode ? 2 : 0) + (longPath ? 1 : 0) + (httpOnly ? 2 : 0) + (badTld ? 2 : 0);

    if (score >= 5) {
      return {
        tone: 'danger',
        message: `High risk (${score}/7). Signals: ${signals.join(', ') || 'None'}.`,
      };
    }

    if (score >= 3) {
      return {
        tone: 'caution',
        message: `Caution (${score}/7). Signals: ${signals.join(', ') || 'None'}.`,
      };
    }

    return {
      tone: 'safe',
      message: `Looks clean (${score}/7). Host: ${host}.`,
    };
  } catch (error) {
    return {
      tone: 'danger',
      message: 'Invalid URL format.',
    };
  }
}

if (controlUrlCheck) {
  controlUrlCheck.addEventListener('click', () => {
    const url = controlUrlInput?.value.trim() ?? '';
    if (window.Android && typeof window.Android.checkUrl === 'function') {
      window.Android.checkUrl(url);
    } else {
      const analysis = evaluateUrl(url);
      if (controlUrlResult) {
        controlUrlResult.textContent = analysis.message;
        controlUrlResult.dataset.tone = analysis.tone;
      }
      showToast(`URL check: ${analysis.tone}`);
    }
  });
}

function setActiveStatus(index) {
  if (!controlStatusItems.length) return;
  controlStatusItems.forEach((item, itemIndex) => {
    if (itemIndex === index) {
      item.dataset.active = 'true';
    } else {
      item.removeAttribute('data-active');
    }
  });
}

if (controlStatusItems.length) {
  setActiveStatus(0);
}

if (controlScan && controlStatusItems.length) {
  controlScan.addEventListener('click', () => {
    const scanningIndex = controlStatusItems.findIndex((item) => item.dataset.state === 'scanning');
    showToast('Scanning installed apps for threats');
    if (scanningIndex !== -1) {
      setActiveStatus(scanningIndex);
      clearTimeout(controlStatusTimeout);
      controlStatusTimeout = setTimeout(() => {
        setActiveStatus(2);
      }, 2600);
    }
  });
}

if (controlModes.length) {
  setControlMode(controlModes[0], true);
}

if (toggle) {
  toggle.addEventListener('click', () => {
    const expanded = toggle.getAttribute('aria-expanded') === 'true';
    toggle.setAttribute('aria-expanded', String(!expanded));
    links.setAttribute('aria-hidden', expanded ? 'true' : 'false');
  });
}

document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape' && toggle?.getAttribute('aria-expanded') === 'true') {
    toggle.setAttribute('aria-expanded', 'false');
    links.setAttribute('aria-hidden', 'true');
    toggle.focus();
  }
});

setInterval(updateStatus, 5000);

links?.setAttribute('aria-hidden', 'true');
if (status) {
  status.textContent = statuses[statusIndex];
}
