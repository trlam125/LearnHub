-- Serialize admin-role changes so the final usable admin cannot be removed.
create table admin_role_guard (id integer primary key);
insert into admin_role_guard(id) values (1);

alter table purchase_order add column access_snapshot_captured boolean default false not null;
alter table purchase_order add column prior_access_active boolean default false not null;
alter table purchase_order add column prior_access_starts_at timestamp with time zone;
alter table purchase_order add column prior_access_expires_at timestamp with time zone;
alter table purchase_order add column prior_access_source_order uuid;
alter table purchase_order add column next_reconcile_at timestamp with time zone;
update purchase_order set next_reconcile_at=created_at;
alter table purchase_order alter column next_reconcile_at set not null;
create index idx_order_reconcile on purchase_order(provider,status,next_reconcile_at,created_at);
