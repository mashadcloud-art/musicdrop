# saavn-proxy

`package.json` here points `main`/`start` at `server.js`, but that file
isn't committed to this repo — so this folder won't run as-is from a fresh
checkout.

The app's live Saavn integration (`SaavnRepository.kt`) currently talks to
a proxy already deployed at `https://filedrop-saavn.fly.dev`, not to
anything built from this folder. If that deployed proxy's source is meant
to live here, `server.js` needs to be added; otherwise this folder is
stale and can be removed.
