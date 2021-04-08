# alvis——命令行工具
#### 使用帮助  
- ```alvis -h```

#### Hive Over HBase外部表字段增加/删除
- 增加字段  
```
alvis -a -A \
    -p hive.database=temp \
    -p hive.table=person \
    -p hive.column.name=gender \
    -p hive.column.type=string \
    -p hbase.column.family.name=data \
    -p hbase.column.name=gender
```
- 删除字段
```
alvis -a -D \
    -p hive.database=temp \
    -p hive.table=person \
    -p hive.column.name=gender \
    -p hive.column.type=string \
    -p hbase.column.family.name=data \
    -p hbase.column.name=gender
```
#### MySQL数据表和Hive数据表结构比较
- 从命令行读取配置  
    - 未添加忽略检查的类型映射
    ```
    alvis -c \
        -p compare.mysql.url="jdbc:mysql://10.2.24.110:3306/vas10?user=秘密&password=不告诉你" \
        -p compare.mysql.table=p_account_\\d0$ \
        -p compare.hive.database=bi_ssa \
        -p compare.hive.table=p_account
    ```
    - 添加忽略检查的类型映射
    ```
    alvis -c \
        -p compare.mysql.url="jdbc:mysql://10.2.24.110:3306/vas10?user=秘密&password=不告诉你" \
        -p compare.mysql.table=p_account_\\d0$ \
        -p compare.hive.database=bi_ssa \
        -p compare.hive.table=p_account \
        -M varchar:string \
        -M timestamp:string
    ```
- 从文件读取配置
  - 带列头的文件  
    - 文件内容  
    ```
    echo -e "url,mt,hdb,ht\njdbc:mysql://10.2.24.110:3306/vas10?user=秘密&password=不告诉你,p_account_\\d0$,bi_ssa,p_account" > with_header.csv
    ```
    - 运行比较  
    ```
    alvis -c -F -H \
        -p compare.file=with_header.csv \
        -p compare.file.delimiter=, \
        -p column.mysql.url=url \
        -p column.mysql.table=mt \
        -p column.hive.database=hdb \
        -p column.hive.table=ht
    ```
  - 不带列头的文件
     - 文件内容  
    ```
    echo -e "jdbc:mysql://10.2.24.110:3306/vas10?user=秘密&password=不告诉你,p_account_\\d0$,bi_ssa,p_account" > without_header.csv
    ```
    - 运行比较  
    ```
    alvis -c -F -I \
        -p compare.file=without_header.csv \
        -p compare.file.delimiter=, \
        -p index.mysql.url=0 \
        -p index.mysql.table=1 \
        -p index.hive.database=2 \
        -p index.hive.table=3
    ```
- 从数据库读取配置
  - 创建配置表
    ```
    create table mysql_hive_compare_config (mysql_url varchar(200),mysql_table_regex varchar(200),hive_database varchar(200),hive_table varchar(200));
    ```
  - 插入测试配置
    ```
    insert into mysql_hive_compare_config values('jdbc:mysql://10.2.24.110:3306/vas10?user=秘密&password=不告诉你','p_account_\\d0$','bi_ssa','p_account');
    ```
  - 运行比较
    ```
    alvis -c -B \
    -p mysql.url="jdbc:mysql://10.2.35.231:3306/test?user=秘密&password=不告诉你" \
    -p mysql.table=mysql_hive_compare_config \
    -p column.mysql.url=mysql_url \
    -p column.mysql.table=mysql_table_regex \
    -p column.hive.database=hive_database \
    -p column.hive.table=hive_table
    ```
- 将比较结果发送邮件
    ```
    alvis -c -e -F -H \
        -p compare.file=with_header.csv \
        -p compare.file.delimiter=, \
        -p column.mysql.url=url \
        -p column.mysql.table=mt \
        -p column.hive.database=hdb \
        -p column.hive.table=ht \
        -p mail.to=recipient@org-name.org
    ```
#### SQL脚本执行器
- 读取HBase表——生成HBase表结构描述JSON
    ```
    alvis -g \
    -p hbase.namespace=default \
    -p hbase.rowkey.alias=code \
    -p hbase.table=person \
    -p hbase.columns=name,age \
    -p hbase.column.name=data:name:string \
    -p hbase.column.age=data:age:int
    ```
- 脚本示例
    ```
    --this is a sql/hql scripts example
    set a=sour;
    set b=ce;
    set source=hp;
    register {"table":{"namespace":"default","name":"default:person"},"rowkey":"key","columns":{"name":{"cf":"data","col":"name","type":"string"},"age":{"cf":"data","col":"age","type":"int"},"code":{"cf":"rowkey","col":"key","type":"string"}}} as ${${a}${b}};
    set age:=select max(age) from ${${a}${b}};
    set destination=temp.person2;
    drop table if exists ${destination};
    create table ${destination} as select * from ${source} where age<=${age};
    select * --a
    from ${destination};--b
    ```
- 变量声明  
>set variable=value
- spark参数设定  
>spark_env spark_config=value
- hive参数设定  
>hive_env hive_config=value
- hbase表注册  
>register hbase_catalog as temp_table
- 变量赋值  
>set variable:=select ...
- 执行脚本
>alvis -s /root/.ivy2/sql/example.sql -p date_dt= -p date_hour= -p date_minute= 
- 导出执行结果
    - 导出到MySQL  
只需在原有`-s/--script script.sql`或`-c/--compare`的基础上增加`-E/--export mysql`并以`-p/--parameters <key=value>`的方式提供`mysql.url`、`mysql.table`和`data.write.mode`3个参数即可
    - 导出到Redis  
只需在原有`-s/--script script.sql`或`-c/--compare`的基础上增加`-E/--export redis`并以`-p/--parameters <key=value>`的方式提供`redis.host`、`redis.port、`redis.key、`redis.password(默认为空字符串)`、`redis.overwrite(默认为false)`和`redis.id`7个参数即可
    - 导出到HDFS文件  
只需在原有`-s/--script script.sql`或`-c/--compare`的基础上增加`-E/--export ssv`并以`-p/--parameters <key=value>`的方式提供`ssv.overwrite`、`ssv.useHeader`、`ssv.delimiter`和`ssv.path`4个参数即可
- 表达式参数  
可以使用`-x variable=expression`将表达式计算的结果作为参数传递给程序，例如：
    - -x number=1+2+3+4+5，程序会自动解析出number=15
    - -x date="2017-08-08".replace("-","")，程序会自动解析出date=20170808
#### Spark日志查看
- 参数  
    - rm.address：resource manager地址
    - app：yarn application的id或关键字
    - log.role：可取driver和executor，默认为driver
    - log.type：可取stderr和stdout，默认为stderr
    - log.length：数值类型，正数x表示忽略前x字节的日志，0表示返回所有日志，负数x表示取最后x字节日志，默认为-4096
- 查看Driver日志
    ```
    alvis -l \
        -p app=application_1501060810163_93203
    ```
- 查看Executor日志
    ```
    alvis -l \
        -p app=application_1501060810163_93203 \
        -p log.role=executor \
    ```
#### 交互式查询
- 启动命令  
```alvis```
- 内部命令，可输入部分命令按tab键查看提示
    - ```!columns <table name>```：列出指定表的所有列信息，相当于```desc <table name>```
    - ```!close```：关闭当前jdbc连接
    - ```!closeAll```：关闭所有jdbc连接
    - ```!connect <jdbc url>```，```!open <jdbc url>```：打开(建立)与指定jdbc url的连接
    - ```!connect #A```：打开配置中形如connection.A.url的连接
    - ```!get```：查看某个配置项的值
    - ```!go <connection id>```，```!# <connection id>```：切换到指定连接
    - ```help```，```?```，```!help```，```!?```：获取使用帮助
    - ```!history```：获取历史命令
    - ```!list```：列出已打开的连接
    - ```!null2Empty <true|false>```：是否将null值以空白字符串输出，相当于```!set print.null2empty=true```或```!set print.null2empty true```
    - ```!outputformat <format>```：设置查询结果的输出格式，相当于```!set print.format=<format>```或```!set print.format <format>```，format可取的值有
        - csv：逗号分隔输出
        - default：带边框的二维表格输出
        - dsv：管道符|分隔输出
        - json：json格式输出，json样式由配置项print.json.pretty控制，false为压缩样式，true为格式化样式
        - tsv：制表符分隔输出
        - vertical：key value格式逐行输出
        - xml_attributes：xml结点属性格式输出
        - xml_elements：xml结点元素格式输出
    - ```!quit```，```!done```，```!exit```：退出程序，若退出前有已打开的连接，会自动先关闭连接，然后退出
    - ```!reconnect```：重新连接当前因某种原因关闭的连接
    - ```!showFunctions```：列出当前连接的自带函数
    - ```!set <configuration key>=或空格<configuration value>```：设置某个配置项的值，立即生效
    - ```!tables```：列出当前连接的数据库中的表