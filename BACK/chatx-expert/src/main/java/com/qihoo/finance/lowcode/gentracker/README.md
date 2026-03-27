# Generate tracker

Generate tracker是ChatX-Expert中负责插件代码生成实现的部分，主要通过自定义模板（基于velocity）来生成各种你想要的代码。通常用于生成Entity、Dao、Service、Controller。

---
### 使用环境
`IntelliJ IDEA Ultimate版（>=2022）`

### 支持的文件模板类型：
> 1. Default
>    1. controller.java.vm
>    2. dao.java.vm
>    3. entity.java.vm
>    4. mapper.xml.vm
>    5. service.java.vm
>    6. serviceImpl.java.vm
>    7. debug.json.vm
> 2. MybatisPlus
>    1. controller.java.vm
>    2. dao.java.vm
>    3. entity.java.vm
>    4. service.java.vm
>    5. serviceImpl.java.vm
> 3. MybatisPlus-Mixed
>    1. controller.java.vm
>    2. dao.java.vm
>    3. entity.java.vm
>    4. mapper.xml.vm
>    5. service.java.vm
>    6. serviceImpl.java.vm
> 4. spring-data-mongodb
>    1. controller.java.vm
>    2. entity.java.vm
>    3. repository.java.vm
>    4. service.java.vm
>    5. serviceImpl.java.vm

### 其他设置
> 1. 支持代码自动格式化
> 2. 支持同名文件自动覆盖模式
> 3. 支持二次生成时进行文件比对处理合并

### 开发计划
> 1.  模板更新，符合数科规范
> 2. 支持基于表数据库生成时设置自定义参数
> 3. 优化二次生成时文件比对
> 4. 代码生成操作记录
> 5. 配置变量值替换
> 6. ...