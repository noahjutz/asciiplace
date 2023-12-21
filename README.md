# Build

```
javac -d build App.java
```

# Package

```
jar -v -c -m MANIFEST.MF -f out.jar -C build .
```

# Run

```
java -cp "postgresql-42.7.1.jar:build" App
```

or

```
java -jar out.jar
```
