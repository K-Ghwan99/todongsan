# Market SQL migration guide

이 디렉터리의 SQL은 기존 로컬 Market DB를 업그레이드할 때 수동으로 적용하는 additive migration이다.

## 로컬 MySQL region_scope migration 적용

기존 Docker MySQL volume을 유지한 상태에서는 `infra/mysql/init/01-market-schema.sql` 변경이 자동 적용되지 않는다. 이미 생성된 로컬 DB에는 필요한 migration을 직접 적용해야 한다.

`region_scope` 작업은 기존 DB 상태에 따라 아래 순서로 적용한다.

```powershell
Get-Content .\docs\market\sql\009_add_market_region_columns.sql | docker exec -i todongsan-mysql mysql -uroot -p1234 market
Get-Content .\docs\market\sql\010_add_market_region_scope.sql | docker exec -i todongsan-mysql mysql -uroot -p1234 market
```

이미 `region_sido`, `region_sigu` 컬럼이 있다면 009는 생략하고 010만 적용한다.

```powershell
Get-Content .\docs\market\sql\010_add_market_region_scope.sql | docker exec -i todongsan-mysql mysql -uroot -p1234 market
```

비밀번호, DB명, 컨테이너명은 각자의 `.env`/compose 설정에 맞게 조정한다.

로컬 데이터를 모두 초기화해도 되는 경우에만 Docker volume을 삭제하고 다시 올린다.

```powershell
docker compose down -v
docker compose up -d
```

`docker compose down -v`는 로컬 MySQL 데이터를 모두 삭제하므로 테스트 데이터나 수동 실험 데이터가 필요한 상황에서는 사용하지 않는다.

## dev seed 주의사항

`dev_seed_market_frontend_scenarios.sql`은 추가 전용 seed다.

- 기존 Market/Prediction 데이터를 삭제하거나 갱신하지 않는다.
- `INSERT IGNORE`를 사용해 기존 로컬 실험 데이터를 보존한다.
- seed Market은 `region_scope`, `region_sido`, `region_sigu`를 명시해 애플리케이션 validation 정책과 일치하는 조합만 생성한다.
