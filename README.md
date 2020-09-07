# auth项目说明文档

## 使用说明
1. 环境准备
* jdk1.8
* 需自行准备mysql、redis环境，并在*.properties文件中进行配置
* spring boot版本2.0.6.RELEASE
* 执行sql文件sys_user_info.sql
2. 内容概括
* spring boot整合shiro的相关配置
* 登录、注册、修改密码等常见功能
* redis单实例与集群方式的配置
此处需注意所支持的spring boot的版本，高版本不兼容JedisCommands，届时自行修改redis相关方法 
3. 注意事项
* 本项目仅用于学习交流
* 代码中如有任何问题，欢迎沟通更正


