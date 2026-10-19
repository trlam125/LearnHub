-- Serialize admin-role changes so the final usable admin cannot be removed.
create table admin_role_guard (id integer primary key);
insert into admin_role_guard(id) values (1);

alter table purchase_order add column access_snapshot_captured bit default 0 not null;
alter table purchase_order add column prior_access_active bit default 0 not null;
alter table purchase_order add column prior_access_starts_at datetime(6);
alter table purchase_order add column prior_access_expires_at datetime(6);
alter table purchase_order add column prior_access_source_order binary(16);
alter table purchase_order add column next_reconcile_at datetime(6);
update purchase_order set next_reconcile_at=created_at;
alter table purchase_order modify column next_reconcile_at datetime(6) not null;
create index idx_order_reconcile on purchase_order(provider,status,next_reconcile_at,created_at);
