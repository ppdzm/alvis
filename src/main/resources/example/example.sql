--this is a sql/hql scripts example
set a=b;
set b=c;
set c=person;
set age:=select max(age) from tmp.${${${a}}};
register {"table":{"namespace":"default","name":"default:person"},"rowkey":"key","columns":{"name":{"cf":"cf","col":"name","type":"string"},"age":{"cf":"cf","col":"age","type":"string"},"code":{"cf":"rowkey","col":"key","type":"string"}}} as p;
insert into table tmp.person select name,age from p where 1=1;
select * --a
from tmp.${${${a}}} --b
where age=${age};--c