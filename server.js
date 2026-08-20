const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const { Pool } = require('pg');

const app = express();
const JWT_SECRET = process.env.JWT_SECRET || 'NEXUS_SUPER_SECRET_KEY_2026_PRODUCTION';

const pool = new Pool({
    connectionString: process.env.DATABASE_URL,
    ssl: { rejectUnauthorized: false }
});

app.use(helmet());
app.use(cors({ origin: '*' }));
app.use(express.json());

const apiLimiter = rateLimit({ windowMs: 15 * 60 * 1000, max: 200 });
app.use('/api/', apiLimiter);

const authenticateToken = (req, res, next) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];
    if (!token) return res.status(401).json({ error: 'Access token required' });

    jwt.verify(token, JWT_SECRET, (err, user) => {
        if (err) return res.status(403).json({ error: 'Invalid or expired token' });
        req.user = user;
        next();
    });
};

app.post('/api/auth/register', async (req, res) => {
    const { email, password, fullName } = req.body;
    if (!email || !password) return res.status(400).json({ error: 'Email and password required' });

    const client = await pool.connect();
    try {
        await client.query('BEGIN');
        const hashedPassword = await bcrypt.hash(password, 12);
        
        const userRes = await client.query(
            'INSERT INTO users (email, password_hash, full_name) VALUES ($1, $2, $3) RETURNING id, email, full_name',
            [email.toLowerCase(), hashedPassword, fullName]
        );
        const newUser = userRes.rows[0];

        await client.query('INSERT INTO wallets (user_id, balance) VALUES ($1, 10000.00)', [newUser.id]);
        
        await client.query('COMMIT');
        
        const token = jwt.sign({ id: newUser.id, email: newUser.email }, JWT_SECRET, { expiresIn: '24h' });
        res.status(201).json({ token, user: newUser });
    } catch (err) {
        await client.query('ROLLBACK');
        if (err.code === '23505') return res.status(400).json({ error: 'Email already exists' });
        res.status(500).json({ error: 'Internal server error' });
    } finally {
        client.release();
    }
});

app.post('/api/auth/login', async (req, res) => {
    const { email, password } = req.body;
    try {
        const userRes = await pool.query('SELECT * FROM users WHERE email = $1', [email.toLowerCase()]);
        if (userRes.rows.length === 0) return res.status(401).json({ error: 'Invalid credentials' });

        const user = userRes.rows[0];
        const validPassword = await bcrypt.compare(password, user.password_hash);
        if (!validPassword) return res.status(401).json({ error: 'Invalid credentials' });

        const token = jwt.sign({ id: user.id, email: user.email }, JWT_SECRET, { expiresIn: '24h' });
        res.json({ token, user: { id: user.id, email: user.email, fullName: user.full_name } });
    } catch (err) {
        res.status(500).json({ error: 'Server error during authentication' });
    }
});

app.get('/api/user/account', authenticateToken, async (req, res) => {
    try {
        const walletRes = await pool.query('SELECT balance, locked_margin FROM wallets WHERE user_id = $1', [req.user.id]);
        const posRes = await pool.query('SELECT * FROM positions WHERE user_id = $1 AND status = $2', [req.user.id, 'OPEN']);
        
        res.json({
            wallet: walletRes.rows[0],
            positions: posRes.rows
        });
    } catch (err) {
        res.status(500).json({ error: 'Failed to fetch account info' });
    }
});

app.post('/api/trade/order', authenticateToken, async (req, res) => {
    const { symbol, side, amount, leverage, currentPrice } = req.body;
    const client = await pool.connect();

    try {
        await client.query('BEGIN');
        const walletRes = await client.query('SELECT balance FROM wallets WHERE user_id = $1 FOR UPDATE', [req.user.id]);
        const balance = parseFloat(walletRes.rows[0].balance);

        if (amount > balance) {
            await client.query('ROLLBACK');
            return res.status(400).json({ error: 'Insufficient balance' });
        }

        await client.query('UPDATE wallets SET balance = balance - $1, locked_margin = locked_margin + $1 WHERE user_id = $2', [amount, req.user.id]);

        const posRes = await client.query(
            `INSERT INTO positions (user_id, symbol, side, entry_price, amount, leverage) 
             VALUES ($1, $2, $3, $4, $5, $6) RETURNING *`,
            [req.user.id, symbol, side, currentPrice, amount, leverage]
        );

        await client.query('COMMIT');
        res.status(201).json({ position: posRes.rows[0] });
    } catch (err) {
        await client.query('ROLLBACK');
        res.status(500).json({ error: 'Order execution failed' });
    } finally {
        client.release();
    }
});

app.post('/api/trade/close', authenticateToken, async (req, res) => {
    const { positionId, closePrice } = req.body;
    const client = await pool.connect();

    try {
        await client.query('BEGIN');
        const posRes = await client.query('SELECT * FROM positions WHERE id = $1 AND user_id = $2 AND status = $3 FOR UPDATE', [positionId, req.user.id, 'OPEN']);
        if (posRes.rows.length === 0) {
            await client.query('ROLLBACK');
            return res.status(404).json({ error: 'Position not found' });
        }

        const pos = posRes.rows[0];
        const priceDiff = closePrice - parseFloat(pos.entry_price);
        const pct = (priceDiff / parseFloat(pos.entry_price)) * (pos.side === 'BUY' ? 1 : -1);
        const pnl = parseFloat(pos.amount) * pct * pos.leverage;
        const totalReturn = parseFloat(pos.amount) + pnl;

        await client.query(
            'UPDATE wallets SET balance = balance + $1, locked_margin = locked_margin - $2 WHERE user_id = $3',
            [Math.max(0, totalReturn), pos.amount, req.user.id]
        );

        await client.query(
            'UPDATE positions SET status = $1, close_price = $2, realized_pnl = $3, closed_at = CURRENT_TIMESTAMP WHERE id = $4',
            ['CLOSED', closePrice, pnl, positionId]
        );

        await client.query('COMMIT');
        res.json({ message: 'Position closed successfully', pnl, returnAmount: totalReturn });
    } catch (err) {
        await client.query('ROLLBACK');
        res.status(500).json({ error: 'Close position failed' });
    } finally {
        client.release();
    }
});

const PORT = process.env.PORT || 5000;
app.listen(PORT, () => console.log(`Server running on port ${PORT}`));
