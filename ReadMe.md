# Zshurl

Simple URL shortener made with Zio, Http4s and Doobie.

Examples:

```
curl -sX POST -H 'Content-Type: application/json' 'localhost/api/shortify' -d '{"url":"http://a.very.long.url"}'

> HTTP 200
> '{"url":"http://localhost/qsVXfyaX"}'
```

```
curl -sX GET -H 'Content-Type: application/json' 'localhost/api/original' -d '{"url":"http://localhost/qsVXfyaX"}'

> HTTP 200
> {"url":"http://a.very.long.url"}
```

### Migration

One can use `service migrate` and `service cleanup` to initialize and cleanup required database tables respectively.

### Configuration

Following environment variables can be used to configure the service:

| Environment Variable | Description |
|-----|------|
| `ZSHURL_DBUSER` or `USER` | Database user name |
| `ZSHURL_LOG_LEVEL` | (optional) Log level (fatal, error, warn, info, debug, trace, off. default: warn |
| `ZSHURL_HOST` | (optional) Hostname (default: localhost) |
| `ZSHURL_PORT` | (optional) Port (default: 8080) |
| `ZSHURL_DBNAME` | (optional) Database name (default: shurl) |
| `ZSHURL_DBPASS` | (optional) Database password if needed |
| `ZSHURL_DBHOST` | (optional) Database host |
| `ZSHURL_DBPORT` | (optional) Database port |

### TODO
- [x] Basic API
- [ ] Stats
- [ ] Authorized access to stats
- [ ] Provide dockerfile && docker-compose.yml
- [ ] Tests
