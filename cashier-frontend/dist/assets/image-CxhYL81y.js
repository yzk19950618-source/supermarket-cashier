function e(t){return t?/^https?:\/\//i.test(t)||/^data:/i.test(t)?t:t.startsWith("/")?`http://localhost:8080${t}`:`http://localhost:8080/${t}`:""}export{e as r};
