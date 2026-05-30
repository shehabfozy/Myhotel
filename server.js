import express from 'express';
import path from 'path';
import { fileURLToPath } from 'url';
import { GoogleGenAI, Type } from '@google/genai';
import dotenv from 'dotenv';

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = 3000;

// Size limit of 10mb to allow file uploads (base64 pictures)
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ limit: '10mb', extended: true }));

// Lazy Initialize GoogleGenAI client nicely
let aiClient = null;
const getAiClient = () => {
  if (!aiClient) {
    const apiKey = process.env.GEMINI_API_KEY;
    if (!apiKey || apiKey === 'MY_GEMINI_API_KEY' || apiKey.startsWith('your_')) {
      throw new Error('لم يتم تهيئة مفتاح API للذكاء الاصطناعي (GEMINI_API_KEY) في السيرفر');
    }
    aiClient = new GoogleGenAI({
      apiKey: apiKey,
      httpOptions: {
        headers: {
          'User-Agent': 'aistudio-build',
        }
      }
    });
  }
  return aiClient;
};

// Check if Gemini API key is configured
app.get('/api/check-key', (req, res) => {
  try {
    getAiClient();
    res.json({ configured: true });
  } catch (error) {
    res.json({ configured: false, message: error.message });
  }
});

// Endpoint for scanning ID cards using Gemini-3.5-flash
app.post('/api/scan-id', async (req, res) => {
  try {
    const { base64Image } = req.body;

    if (!base64Image) {
      return res.status(400).json({ error: 'لم يتم إرسال صورة الهوية بطريقة صحيحة' });
    }

    const ai = getAiClient();

    const prompt = `
      Analyze this uploaded ID card, Passport, or Residency ID card. 
      Extract characters precisely and return a JSON object with the following fields:
      - "name": The guest's full name in classical clear Arabic (matching how it is printed on the ID, but clean). If printed in English only, return the English name.
      - "id_number": The exact identification number digit-by-digit.
      - "id_type": This must be exactly one of: "هوية وطنية" (for Saudi/local National ID), "جواز سفر" (for Passport), or "هوية مقيم" (for residency/iqama card).
      - "nationality": National origin in Arabic.
      
      Return ONLY the raw JSON object. Do NOT wrap it in Markdown or any other wrapper.
    `;

    // Process base64 data: remove MIME prefix if present
    const cleanBase64 = base64Image.replace(/^data:image\/\w+;base64,/, "");

    const response = await ai.models.generateContent({
      model: 'gemini-3.5-flash',
      contents: [
        {
          inlineData: {
            mimeType: 'image/jpeg',
            data: cleanBase64
          }
        },
        prompt
      ],
      config: {
        responseMimeType: 'application/json',
        responseSchema: {
          type: Type.OBJECT,
          properties: {
            name: { type: Type.STRING, description: "Guest's full name in classical Arabic" },
            id_number: { type: Type.STRING, description: "Exact identification or passport number" },
            id_type: { type: Type.STRING, description: "Type of identification" },
            nationality: { type: Type.STRING, description: "Nationality in Arabic" }
          },
          required: ["name", "id_number", "id_type"]
        }
      }
    });

    const responseText = response.text;
    console.log('Gemini scan raw text:', responseText);

    try {
      const parsedData = JSON.parse(responseText.trim());
      res.json(parsedData);
    } catch (e) {
      console.error('Failed to parse Gemini JSON output', e);
      res.status(500).json({ error: 'فشل تحليل الاستجابة من الذكاء الاصطناعي ككائن JSON منظم', detail: responseText });
    }

  } catch (error) {
    console.error('Gemini Scan API error:', error);
    res.status(500).json({ error: error.message || 'حدث خطأ غير متوقع أثناء معالجة صورة الهوية' });
  }
});

// Serve built React files from 'dist'
app.use(express.static(path.join(__dirname, 'dist')));

// Fallback all other routes to index.html for React SPA
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'dist', 'index.html'));
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Server is running in producción on port ${PORT}`);
});
