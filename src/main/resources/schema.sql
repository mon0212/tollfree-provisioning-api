create table if not exists customers (id varchar(36) primary key, name varchar(255) not null, email varchar(255) not null unique, created_at timestamp with time zone not null);
create index if not exists ix_customer_email on customers(email);
create table if not exists telecom_numbers (number varchar(20) primary key, status varchar(20) not null, customer_id varchar(36), version bigint not null, updated_at timestamp with time zone not null, constraint fk_number_customer foreign key(customer_id) references customers(id));
create index if not exists ix_number_status on telecom_numbers(status); create index if not exists ix_number_customer on telecom_numbers(customer_id);
create table if not exists provisioning_requests (id varchar(36) primary key, customer_id varchar(36) not null, number varchar(20) not null, created_at timestamp with time zone not null, constraint uk_provision_customer_number unique(customer_id,number));
create table if not exists audit_logs (id varchar(36) primary key, number varchar(20) not null, action varchar(255) not null, created_at timestamp with time zone not null); create index if not exists ix_audit_number on audit_logs(number);
