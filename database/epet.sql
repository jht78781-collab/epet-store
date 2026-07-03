drop database if exists epet;
create database epet default character set utf8mb4 collate utf8mb4_general_ci;
use epet;

create table petstore (
    id int primary key auto_increment,
    name varchar(20) not null,
    password varchar(20) null,
    balance int not null
);

create table petowner (
    id int primary key auto_increment,
    name varchar(20) not null,
    password varchar(20) null,
    money int not null
);

create table pet (
    id int primary key auto_increment,
    name varchar(20) not null,
    typename varchar(20),
    health int,
    love int,
    birthday datetime,
    owner_id int,
    store_id int,
    foreign key (owner_id) references petowner(id),
    foreign key (store_id) references petstore(id)
);

create table account (
    id int primary key auto_increment,
    deal_type int,
    pet_id int,
    seller_id int,
    buyer_id int,
    price int,
    deal_time datetime,
    foreign key (pet_id) references pet(id)
);

insert into petstore values (default, '长沙宠爱一生宠物店', '123456', 600);
insert into petstore values (default, '武汉偶偶爱宠物店', '123456', 800);

insert into petowner values (default, 'tuling', '123456', 400);
insert into petowner values (default, 'itboy', '123456', 200);

insert into pet values (default, '花花', 'dog', 1, 50, '2010-02-02', 1, 1);
insert into pet values (default, '贝贝', 'penguin', 1, 60, '2009-08-08', null, 2);
insert into pet values (default, '成成', 'dog', 1, 70, '2013-03-03', null, 1);
insert into pet values (default, '露露', 'bird', 1, 70, '2014-03-03', null, 1);
insert into pet values (default, '老虎欧欧', 'tiger', 1, 20, '2010-03-03', 2, 1);
insert into pet values (default, '老虎环环', 'tiger', 1, 20, '2010-03-03', null, 1);
insert into pet values (default, '老虎美美', 'tiger', 1, 11, '2010-04-23', null, 1);
insert into pet values (default, '狮子', 'lion', 0, 25, '2010-04-23', null, 2);

drop procedure if exists ownerBuyProc;
delimiter $$
create procedure ownerBuyProc(
    in ownerId int,
    in storeId int,
    in petId int,
    in dealCost int,
    out result int
)
begin
    declare exit handler for sqlexception
    begin
        rollback;
        set result = -1;
    end;

    set result = 0;
    start transaction;

    update petowner
       set money = money - dealCost
     where id = ownerId
       and money >= dealCost;
    set result = result + row_count();

    update petstore
       set balance = balance + dealCost
     where id = storeId;
    set result = result + row_count();

    update pet
       set owner_id = ownerId
     where id = petId
       and store_id = storeId
       and owner_id is null;
    set result = result + row_count();

    insert into account(deal_type, pet_id, seller_id, buyer_id, price, deal_time)
    values (1, petId, storeId, ownerId, dealCost, now());
    set result = result + row_count();

    if result = 4 then
        commit;
    else
        rollback;
    end if;
end $$
delimiter ;
