Invoke-RestMethod -Method POST -Uri http://localhost:8080/api/v1/templates/record_types/defaults_aus_gov:email/apply?includeDependencies=true

$FileStoreBody = @{
    type = "filestore_local:local"
    properties = @{
        "rootDir" = "./store"
    }
}
$FileStore = Invoke-RestMethod -Method POST -Uri http://localhost:8080/api/v1/file_stores -Body ($FileStoreBody | ConvertTo-Json -Compress) -ContentType application/json

Invoke-RestMethod -Method PUT -Uri "http://localhost:8080/api/v1/config/workgroup.default_file_store" -Body $FileStore.id

$RecordBody = @{
    type = "defaults_aus_gov:email"
    properties = @{
        "builtin:notes" = "Test"
    }
}
$Record = Invoke-RestMethod -Method POST -Uri http://localhost:8080/api/v1/records -Body ($RecordBody | ConvertTo-Json -Compress) -ContentType application/json

Invoke-RestMethod -Method PUT -Uri "http://localhost:8080/api/v1/records/$( $Record.id )/revisions/1.0" -InFile "./COMMITING.md"