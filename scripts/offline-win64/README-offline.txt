Offline Single-PC Package (Windows)

This tree is for single-machine offline run (zip it for distribution).

Contents:
- runtime/jre/         JRE 17 (unzip here)
- mysql/               portable MySQL (unzip here)
- backend/cashier-backend.jar
- frontend/dist/       Vite build (also bundled in jar; start.bat loads both)
- db/README.txt        notes: incremental SQL under db/incremental/
- db/incremental/      same SQL as text reference; see README.md
- dbsql/               run-incremental.bat + copies of *.sql: double-click to apply via bundled mysql (MySQL must be running)
- scripts/init/init-db.bat      first install: copy your backup init.sql into db\ then run
- scripts/init/init-region.bat  regions: copy region-init.sql from backup into db\ then run
- scripts/init/init-all.bat     runs both
- scripts/runtime/start.bat | stop.bat

Order:
1) First time: copy init.sql and region-init.sql from your backup into db\, then scripts\init\init-all.bat
2) Daily: scripts\runtime\start.bat
3) Stop: scripts\runtime\stop.bat
