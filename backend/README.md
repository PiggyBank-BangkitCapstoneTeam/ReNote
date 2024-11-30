# PiggyBank's ReNote Backend API Service


## Notes

Error DeprecationWarning untuk module punycode berasal dari package `node-fetch` yang digunakan oleh 3 package lainnya:
```
google-gax@4.4.1 dan teeny-request@9.0.0 dan gaxios@6.7.1
|- node-fetch@2.7.0
	|- whatwg-url@5.0.0
```

Tidak ada solusi untuk sementara ini selain [menunggu update](https://github.com/googleapis/gax-nodejs/issues/1568) atau paksa override di package.json (_tidak disarankan_).