/**
 * 将 Vite 构建产物复制到 Spring Boot classpath 静态目录，
 * 避免仅启动后端时仍在使用旧的 resources/static 导致白屏或 chunk 不匹配。
 */
import { cpSync, existsSync, mkdirSync, rmSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const frontendRoot = join(__dirname, '..')
const dist = join(frontendRoot, 'dist')
const target = join(frontendRoot, '..', 'cashier-backend', 'src', 'main', 'resources', 'static')

if (!existsSync(dist)) {
  console.error('缺少 dist 目录，请先执行: npm run build')
  process.exit(1)
}

mkdirSync(dirname(target), { recursive: true })
if (existsSync(target)) rmSync(target, { recursive: true })
mkdirSync(target, { recursive: true })
cpSync(dist, target, { recursive: true })
console.log(`已同步: ${dist} -> ${target}`)
