#MakeFile Teste para setup do ambiente

criarERodarContainerBD:
	@ echo "Criando e inicializando container MySql"
	docker run --name seazs-mysql -e MYSQL_ROOT_PASSWORD=system mysql
	@ echo "Inicializando banco de dados do sistema"
	./gradlew flywayMigrate -i

iniciarContainerBD:
	@ echo "Rodando container MySql Existente"
	docker start seazs-mysql
	@ echo "Criando/Atualizando banco de dados do sistema"
	./gradlew flywayMigrate -i
