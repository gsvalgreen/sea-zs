#MakeFile Teste para setup do ambiente

criarERodarContainerBD:
	@ echo "Criando e inicializando container MySql"
	docker run --name seazs-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=system mysql
	@ echo "Inicializando banco de dados do sistema"
	export SEAZS_MYSQL_DB_URL=jdbc:mysql://172.17.0.2:3306
	./gradlew flywayMigrate -i

iniciarContainerBD:
	@ echo "Rodando container MySql Existente"
	docker start seazs-mysql
	@ echo "Criando/Atualizando banco de dados do sistema"
	./gradlew flywayMigrate -i
