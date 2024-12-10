## Request (Backend -> Kode ML)
```
{
	"note_id": "yfQdsNHR9ovXb8Vs1VwgPxrVm61zx8um",
	"photo_id": "PdMPeu4F2N6yFyeDjHXsQdj4qh5xLsjK"
}
```

## Response (Backend <- Kode ML)
### Success Response
```
{
    "success": true,
    "note_id": "yfQdsNHR9ovXb8Vs1VwgPxrVm61zx8um",
    "photo_id": "PdMPeu4F2N6yFyeDjHXsQdj4qh5xLsjK",
    "result": {
        "item": [
            "Renfred Leeman 1  123.456"
        ],
		"shop": "Agustinus Shop"
        "total": "123.456"
    }
}
```

### Failure Response
```
{
    "success": false,
    "error": "..."
}
```