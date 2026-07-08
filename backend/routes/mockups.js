const express = require('express');
const router = express.Router();
const fs = require('fs');
const path = require('path');
const { exec } = require('child_process');

// POST capture screen from connected phone/emulator using adb
router.post('/capture', (req, res) => {
  const homeDir = process.env.HOME || '/home/anonymous';
  const adbPath = path.join(homeDir, 'Android/Sdk/platform-tools/adb');
  const timestamp = Date.now();
  const filename = `capture-${timestamp}.png`;
  const localPath = path.join(__dirname, '../../ui-lab/public/screenshots', filename);
  
  // 1. Trigger screencap on device
  exec(`"${adbPath}" shell screencap -p /sdcard/screencap.png`, (err) => {
    if (err) {
      return res.status(500).json({ error: 'Failed to capture screen: ' + err.message });
    }
    
    // 2. Pull screen to local screenshots folder
    exec(`"${adbPath}" pull /sdcard/screencap.png "${localPath}"`, (err) => {
      if (err) {
        return res.status(500).json({ error: 'Failed to pull screen: ' + err.message });
      }
      
      // 3. Update screens.json configuration
      const configPath = path.join(__dirname, '../../ui-lab/src/config/screens.json');
      let screens = [];
      try {
        screens = JSON.parse(fs.readFileSync(configPath, 'utf8'));
      } catch(e) {}
      
      const newScreen = {
        id: `capture-${timestamp}`,
        image: filename,
        title: `Real Capture - ${new Date().toLocaleTimeString()}`,
        description: "Live screenshot captured from device.",
        order: screens.length + 1,
        actions: ["Interact with App", "Inspect UI Layout", "Validate Components"]
      };
      
      screens.push(newScreen);
      fs.writeFileSync(configPath, JSON.stringify(screens, null, 2));
      
      res.json({ success: true, screen: newScreen });
    });
  });
});

const DATA_FILE = path.join(__dirname, '../data/launch-mockups.json');
const IMAGE_DIR = path.join(__dirname, '../public/launch-images');

// Helper to read data
const readData = () => {
  if (!fs.existsSync(DATA_FILE)) {
    fs.writeFileSync(DATA_FILE, JSON.stringify([]));
  }
  try {
    return JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
  } catch (e) {
    return [];
  }
};

// Helper to write data
const writeData = (data) => {
  fs.writeFileSync(DATA_FILE, JSON.stringify(data, null, 2));
};

// GET all launch mockups
router.get('/', (req, res) => {
  const data = readData();
  res.json(data);
});

// GET list of custom background images
router.get('/backgrounds', (req, res) => {
  const bgDir = path.join(__dirname, '../../ui-lab/public/backgrounds');
  if (!fs.existsSync(bgDir)) {
    return res.json([]);
  }
  try {
    const files = fs.readdirSync(bgDir).filter(file => /\.(png|jpe?g|svg|webp)$/i.test(file));
    res.json(files);
  } catch (e) {
    res.json([]);
  }
});

// GET dynamic screens config list
router.get('/screens', (req, res) => {
  const configPath = path.join(__dirname, '../../ui-lab/src/config/screens.json');
  try {
    const data = JSON.parse(fs.readFileSync(configPath, 'utf8'));
    res.json(data);
  } catch (e) {
    res.json([]);
  }
});

// POST save a launch mockup
router.post('/', (req, res) => {
  const { id, title, description, theme, image } = req.body;
  if (!id || !title) {
    return res.status(400).json({ error: 'ID and Title are required' });
  }

  let finalImagePath = '';
  // If base64 image data is provided, save it as a PNG file
  if (image && image.startsWith('data:image')) {
    const base64Data = image.replace(/^data:image\/png;base64,/, '');
    const filename = `${id}-${Date.now()}.png`;
    const filepath = path.join(IMAGE_DIR, filename);
    
    // Create dir if it doesn't exist
    if (!fs.existsSync(IMAGE_DIR)) {
      fs.mkdirSync(IMAGE_DIR, { recursive: true });
    }
    
    fs.writeFileSync(filepath, base64Data, 'base64');
    finalImagePath = `/launch-images/${filename}`;
  }

  const data = readData();
  const existingIndex = data.findIndex(item => item.id === id);

  const mockupItem = {
    id,
    title,
    description,
    theme,
    imagePath: finalImagePath || (existingIndex >= 0 ? data[existingIndex].imagePath : ''),
    updatedAt: new Date().toISOString()
  };

  if (existingIndex >= 0) {
    // Delete old image if a new one is being uploaded
    if (finalImagePath && data[existingIndex].imagePath) {
      const oldPath = path.join(__dirname, '../public', data[existingIndex].imagePath);
      if (fs.existsSync(oldPath)) {
        try { fs.unlinkSync(oldPath); } catch(e) {}
      }
    }
    data[existingIndex] = mockupItem;
  } else {
    data.push(mockupItem);
  }

  writeData(data);
  res.json({ success: true, item: mockupItem });
});

// DELETE a launch mockup
router.delete('/:id', (req, res) => {
  const { id } = req.params;
  const data = readData();
  const index = data.findIndex(item => item.id === id);
  
  if (index >= 0) {
    const item = data[index];
    if (item.imagePath) {
      const oldPath = path.join(__dirname, '../public', item.imagePath);
      if (fs.existsSync(oldPath)) {
        try { fs.unlinkSync(oldPath); } catch(e) {}
      }
    }
    data.splice(index, 1);
    writeData(data);
    return res.json({ success: true });
  }
  
  res.status(404).json({ error: 'Mockup not found' });
});

module.exports = router;
