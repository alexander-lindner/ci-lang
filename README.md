```bash
./build.sh
docker run -ti --rm --volume $(pwd):/build -w /build alexanderlindner/cish:latest bash
./build.cish
```