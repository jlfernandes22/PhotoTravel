const express = require("express");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const morgan = require("morgan");
const multer = require("multer");
const fs = require("fs");
const path = require("path");

const app = express();
app.use(express.json());
app.use(morgan("dev"));

const SECRET_KEY = "minha_chave_secreta_super_segura";

// --------------------
// Arquivos que simulam DB
// --------------------
const USERS_FILE = path.join(__dirname, "users.txt");
const PHOTOS_FILE = path.join(__dirname, "photos.json");

// Funções de leitura/escrita
function readUsersFromFile() {
    if (!fs.existsSync(USERS_FILE)) return [];
    const data = fs.readFileSync(USERS_FILE, "utf-8");
    if (!data) return [];
    try { return JSON.parse(data); }
    catch (err) { console.error("Erro ao ler users.txt:", err.message); return []; }
}

function writeUsersToFile(users) {
    fs.writeFileSync(USERS_FILE, JSON.stringify(users, null, 2));
}

function readPhotosFromFile() {
    if (!fs.existsSync(PHOTOS_FILE)) return {};
    const data = fs.readFileSync(PHOTOS_FILE, "utf-8");
    if (!data) return {};
    try { return JSON.parse(data); }
    catch (err) { console.error("Erro ao ler photos.json:", err.message); return {}; }
}

function writePhotosToFile(photosData) {
    fs.writeFileSync(PHOTOS_FILE, JSON.stringify(photosData, null, 2));
}

// --------------------
// In-memory DBs
// --------------------
const collections = [];
const photosData = readPhotosFromFile(); // { userId: [ { collectionId, latitude, longitude, base64, titulo } ] }

// --------------------
// Auth Middleware
// --------------------
function authMiddleware(req, res, next) {
    const authHeader = req.headers.authorization;
    const token = authHeader?.split(" ")[1];
    if (!token) return res.status(401).json({ error: "Token em falta" });

    try {
        req.user = jwt.verify(token, SECRET_KEY); // { id, email }
        next();
    } catch {
        return res.status(401).json({ error: "Token inválido" });
    }
}

// --------------------
// Multer (temp upload)
// --------------------
const upload = multer({ dest: "temp/" });

// --------------------
// REGISTER
// --------------------
app.post("/register", async (req, res) => {
    const { email, password, passwordConfirmacao } = req.body;

const users = readUsersFromFile();
    if (users.find(u => u.email === email)) return res.status(400).json({ error: "Email já registado" });

    // 4. Verificação de formato de e-mail (Regex simples)
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) return res.status(400).json({ error: "Formato de e-mail inválido" });

    

    // 1. Verificações básicas de existência
    if (!email || !password || !passwordConfirmacao) return res.status(400).json({ error: "Preenche todos os campos!" });
    
    // 2. Verificação de coincidência de passwords
    if (password !== passwordConfirmacao) return res.status(400).json({ error: "Passwords não coincidem" });

    // 3. Verificação do tamanho da password (min 8 caracteres)
    if (password.length < 8) return res.status(400).json({ error: "A password deve ter pelo menos 8 caracteres" });


    const hashedPassword = await bcrypt.hash(password, 10);
    const newUser = { id: users.length + 1, email, password: hashedPassword };
    users.push(newUser);
    writeUsersToFile(users);

    res.status(201).json({ message: "Utilizador criado com sucesso" });
});

// --------------------
// LOGIN
// --------------------
app.post("/login", async (req, res) => {
    const { email, password } = req.body;
    const users = readUsersFromFile();
    const user = users.find(u => u.email === email);
    if (!user) return res.status(400).json({ error: "Email ou password incorretos" });

    if (!(await bcrypt.compare(password, user.password))) return res.status(400).json({ error: "Email ou password incorretos" });

    const token = jwt.sign({ id: user.id, email: user.email }, SECRET_KEY, { expiresIn: "1h" });
    res.json({ token, user: { id: user.id, email: user.email } });
});

// --------------------
// CREATE COLLECTION
// --------------------
app.post("/collections", authMiddleware, (req, res) => {
    const { title, date } = req.body;
    if (!title || !date) return res.status(400).json({ error: "Dados incompletos" });

    const collection = { id: collections.length + 1, userId: req.user.id, title, date };
    collections.push(collection);
    res.status(201).json(collection);
});

// --------------------
// GET COLLECTIONS
// --------------------
app.get("/collections", authMiddleware, (req, res) => {
    const userCollections = collections.filter(c => c.userId === req.user.id);
    res.json(userCollections.map(c => ({ id: c.id, titulo: c.title })));
});

// --------------------
// UPLOAD PHOTO (Base64 direto)
app.post("/photos", authMiddleware, upload.single("image"), (req, res) => {
    const { latitude, longitude, collectionId } = req.body;
    if (!req.file || !latitude || !longitude || !collectionId) return res.status(400).json({ error: "Dados incompletos" });

    // Ler o ficheiro temporário e converter para Base64
    const imageBuffer = fs.readFileSync(req.file.path);
    const base64Image = imageBuffer.toString("base64");
    fs.unlinkSync(req.file.path); // Apagar ficheiro temporário

    const userId = String(req.user.id);
    if (!photosData[userId]) photosData[userId] = [];

    const photo = {
        id: Date.now(),
         collectionId: Number(collectionId),
         latitude: Number(latitude),
         longitude: Number(longitude),
         base64: base64Image,
         titulo: `Foto ${photosData[userId].length + 1}`,
         tituloPersonalizado: null
    };

    photosData[userId].push(photo);
    writePhotosToFile(photosData);

    res.status(201).json(photo);
});

// --------------------
// GET PHOTOS (Base64)
// --------------------
app.get("/photos", authMiddleware, (req, res) => {
    const { collectionId } = req.query;
    const userId = String(req.user.id);
    const userPhotos = photosData[userId] || [];

    const filteredPhotos = userPhotos.filter(p => !collectionId || p.collectionId == collectionId);
    res.json(filteredPhotos);
});

// --------------------
// DELETE PHOTO
// --------------------
app.delete("/photos/:id", authMiddleware, (req, res) => {
    const photoId = Number(req.params.id);
    const userId = String(req.user.id);

    if (!photosData[userId]) {
        return res.status(404).json({ error: "Foto não encontrada" });
    }

    const initialLength = photosData[userId].length;

    photosData[userId] = photosData[userId].filter(
        photo => photo.id !== photoId
    );

    if (photosData[userId].length === initialLength) {
        return res.status(404).json({ error: "Foto não encontrada" });
    }

    writePhotosToFile(photosData);

    res.json({ message: "Foto apagada com sucesso" });
});

// --------------------
// DELETE COLLECTION (NOVO)
// --------------------
app.delete("/collections/:id", authMiddleware, (req, res) => {
    const collectionId = Number(req.params.id);
    const userId = req.user.id;

    // 1. Verificar se a coleção existe e pertence ao user
    const colIndex = collections.findIndex(c => c.id === collectionId && c.userId === userId);

    if (colIndex === -1) {
        return res.status(404).json({ error: "Coleção não encontrada" });
    }

    // 2. Apagar a coleção da memória
    collections.splice(colIndex, 1);

    // 3. Apagar todas as fotos dessa coleção no JSON
    const userIdStr = String(userId);
    if (photosData[userIdStr]) {
        // Mantém apenas as fotos que NÃO pertencem a esta coleção
        photosData[userIdStr] = photosData[userIdStr].filter(photo => photo.collectionId !== collectionId);
        
        // Gravar no ficheiro
        writePhotosToFile(photosData);
    }

    res.json({ message: "Coleção e respetivas fotos apagadas com sucesso" });
});

// --------------------
// UPDATE COLLECTION (RENOMEAR)
// --------------------
app.put("/collections/:id", authMiddleware, (req, res) => {
    const collectionId = Number(req.params.id);
    const userId = req.user.id;
    const { newTitle } = req.body;

    if (!newTitle) return res.status(400).json({ error: "Novo título é obrigatório" });

    // 1. Atualizar na lista de coleções
    const collection = collections.find(c => c.id === collectionId && c.userId === userId);
    if (!collection) {
        return res.status(404).json({ error: "Coleção não encontrada" });
    }
    collection.title = newTitle; // Atualiza o título da coleção

    // 2. Atualizar nas fotos (photos.json)
    const userIdStr = String(userId);
    if (photosData[userIdStr]) {
        let mudouAlgo = false;
        
        photosData[userIdStr].forEach(photo => {
            if (photo.collectionId === collectionId) {
                // Atualiza o campo que desejas. 
                // Nota: Normalmente o nome do álbum não se guarda na foto, mas como pediste:
                photo.tituloPersonalizado = newTitle; 
                mudouAlgo = true;
            }
        });

        if (mudouAlgo) {
            writePhotosToFile(photosData);
        }
    }

    res.json({ message: "Coleção renomeada com sucesso", collection });
});

// --------------------
// SERVER START
// --------------------
const PORT = process.env.PORT || 25979;
app.listen(PORT, "0.0.0.0", () => console.log(`🚀 Servidor a voar na porta ${PORT}`));