#MakeFile Teste para setup do ambiente

criarERodarContainerBD: 
	docker run --name seazs-mysql -e MYSQL_ROOT_PASSWORD=system mysql
	@ echo "agora você deve se conectar no Banco de Dados e criar o schema seazs"

iniciarContainerBD:
	docker start seazs-mysql
