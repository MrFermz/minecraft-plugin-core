# minecraft-plugin-core

Shared core module สำหรับ ecosystem นี้ — **load เป็น plugin แยกบน server** (ไม่ shade เข้าแต่ละ plugin) เพื่อให้ connection pool / web-config client มีอยู่ชุดเดียวจริง ๆ

Feature plugin ทุกตัว depend on ตัวนี้แบบ `compileOnly(project(":minecraft-plugin-core"))` + ใส่ `depend: [Core]` ใน `plugin.yml` (ชื่อ plugin ที่โชว์ใน `/pl` = `Core`) แล้วคุยกันตอน runtime ผ่าน Bukkit `ServicesManager`

ห้ามใส่ logic เกมเฉพาะ plugin ใด ๆ ไว้ที่นี่ — ดู convention ส่วนกลางที่ [CLAUDE.md](../CLAUDE.md)

## สิ่งที่มีให้ (package `com.mrfermz.mcplugins.core`)

| ส่วน | คลาส | ใช้ทำอะไร |
|------|------|-----------|
| `CorePlugin` | `CorePlugin` | entry point (`JavaPlugin`) — bootstrap shared infra, ไม่มี game logic |
| `CoreApi` | `CoreApi` | ตัวช่วย lookup service จาก `ServicesManager` เช่น `CoreApi.economy(server)` |
| `EcosystemData` | `EcosystemData` | resolve config/data รวมที่ `plugins/antitle/` — config แบนเป็น `plugins/antitle/<module>.yml` (แทน `getDataFolder()` ที่ override ไม่ได้) ดู [Config directory บน server](../CLAUDE.md#config-directory-บน-server) |
| `api/` | `EconomyService`, `EconomyResponse` | สัญญา economy ที่ plugin อื่นเรียกใช้ (impl อยู่ใน money) ใช้ `BigDecimal` ห้าม `double` |
| `db/` | `DatabaseService`, `HikariDatabaseService`, `Dialect`, `DatabaseSettings` | shared DB กลาง — HikariCP pool เดียว, เลือก engine ได้ (sqlite/postgresql/mysql/mariadb); plugin อื่นขอ `DataSource` + `dialect()` ผ่าน `CoreApi.database(server)` + `tablePrefix("<module>")` |
| `config/` | `ConfigClient` | **seam เท่านั้น** — interface อ่านค่าจาก `webconfig/` (cache + refresh) ยังไม่มี impl |
| `log/` | `PluginLog` | wrapper รอบ Bukkit logger ให้ทุก plugin format log เหมือนกัน (`PluginLog.of(plugin)`, รองรับ `{}` placeholder) |

## วิธีให้ plugin อื่นเรียก economy

```java
EconomyService eco = CoreApi.economy(getServer())
        .orElseThrow(() -> new IllegalStateException("Money plugin ยังไม่ load"));
BigDecimal bal = eco.getBalance(player.getUniqueId());
eco.withdraw(player.getUniqueId(), new BigDecimal("50"));
```

## โฟลเดอร์ config รวม (`EcosystemData`)

ทุก plugin แชร์โฟลเดอร์เดียวบน server: `plugins/antitle/` (ไม่ใช่ `plugins/<PluginName>/` ต่อตัว) — config แต่ละ plugin เป็นไฟล์แบน `plugins/antitle/<module>.yml` ไม่ทำ subfolder เพราะ `getDataFolder()` ของ Bukkit เป็น `final` core เลยเป็นเจ้าของ helper ที่ resolve path ให้:

```java
FileConfiguration cfg = EcosystemData.config(this, "money"); // plugins/antitle/money.yml (seed จาก jar)
FileConfiguration global = EcosystemData.config(this);       // plugins/antitle/config.yml (core เป็นเจ้าของ)
File dir = EcosystemData.folder(this, "money");              // plugins/antitle/money/ (เฉพาะถ้าต้องการ dir เก็บไฟล์ข้อมูล)
```

feature plugin **ห้ามเรียก `getDataFolder()`/`getConfig()`/`saveDefaultConfig()` ตรง ๆ** — ดูรายละเอียดเต็มที่ [CLAUDE.md → Config directory บน server](../CLAUDE.md#config-directory-บน-server)

## Logging

`PluginLog` (`PluginLog.of(this)`) เป็น wrapper รอบ Bukkit logger ให้ทุก plugin format log เหมือนกัน (รองรับ `{}` placeholder) — print ลง server console ตามปกติ

> ตอนนี้ **ไม่มี centralized log persistence บน core** — สิ่งที่ persist ลง DB ตอนนี้คือ **money transaction อย่างเดียว** (ตาราง `money_transactions` เขียนผ่าน core `DatabaseService` เหมือนตารางอื่น) ดู [minecraft-plugin-money README](../minecraft-plugin-money/README.md#transaction-log-audit-trail)

## สถานะ

- ✅ API surface (`EconomyService`/`EconomyResponse`), `CoreApi`, `EcosystemData`, `PluginLog`
- ✅ `DatabaseService` wired — `HikariDatabaseService` รองรับ **sqlite (default) / postgresql (แนะนำ production) / mysql / mariadb** เลือกผ่าน `database.type` ใน global `config.yml`; driver โหลด runtime ผ่าน Paper `libraries:` (ไม่ shade); plugin อื่นใช้ `dialect()` เลือก SQL ที่ถูก engine
- ⏳ `ConfigClient` เป็น interface placeholder (รอ `webconfig/`); ยังไม่มี migration กลาง (Flyway) — แต่ละ plugin `CREATE TABLE IF NOT EXISTS` ไปก่อน

## Build

```
./gradlew :minecraft-plugin-core:build
```
