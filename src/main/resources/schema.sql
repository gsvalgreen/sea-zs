create schema seazs;

create table tb_curso
(
	id_curso int auto_increment
		primary key,
	modalidade_curso int not null,
	nome_curso varchar(255) not null,
	qtd_semestre int not null
);

create table tb_disciplina
(
	id_disciplina int auto_increment
		primary key,
	codigo_siga varchar(255) not null,
	nome_disciplina varchar(255) not null,
	id_curso int not null,
	constraint FK1d3cfdsljpcvem7s9bmlyniua
		foreign key (id_curso) references tb_curso (id_curso)
);

create table tb_papel
(
	id_papel int auto_increment
		primary key,
	descricao_papel varchar(255) null,
	nome_papel varchar(255) not null
);

create table tb_usuario
(
	id_usuario int auto_increment
		primary key,
	adm bit not null,
	celular_usuario varchar(255) not null,
	data_nascimento date not null,
	email_usuario varchar(255) not null,
	endereco_usuario varchar(255) not null,
	genero_usuario varchar(255) not null,
	matricula_usuario varchar(255) null,
	nome_usuario varchar(255) not null,
	rg_usuario varchar(255) not null,
	semestre int null,
	senha varchar(255) not null,
	tipo_usuario int not null,
	turno char null,
	id_curso int null,
	constraint UK_fudv57wn1m68hm816yebp0sai
		unique (email_usuario),
	constraint FK1m9r3l1txsq7clps1cp4vo9n9
		foreign key (id_curso) references tb_curso (id_curso)
);

create table tb_evento
(
	id_evento int auto_increment
		primary key,
	data_inclusao date not null,
	descricao_evento varchar(255) null,
	data_fim datetime not null,
	data_inicio datetime not null,
	nome_evento varchar(255) not null,
	id_curso int null,
	id_usuario_criador int not null,
	constraint FK33g9uxjfoir3k6c1jndhu02c9
		foreign key (id_usuario_criador) references tb_usuario (id_usuario),
	constraint FKt0qaq7em9mvt9uvjyl78lu159
		foreign key (id_curso) references tb_curso (id_curso)
);

create table tb_agendamento
(
	id_agendamento int auto_increment
		primary key,
	data_fim datetime not null,
	data_inicio datetime not null,
	pontuacao_staff int not null,
	id_evento int not null,
	id_usuario_organizador int not null,
	constraint FK51d1kmpnv999mbn5kyum5rcew
		foreign key (id_evento) references tb_evento (id_evento),
	constraint FK6elac648q29pvs9tr0s3rbtle
		foreign key (id_usuario_organizador) references tb_usuario (id_usuario)
);

create table tb_atividade
(
	id_atividade int auto_increment
		primary key,
	carga_horaria int not null,
	categoria_atividade varchar(255) not null,
	descricao_atividade varchar(255) null,
	data_fim datetime not null,
	flag_continua bit null,
	flag_cumulativa bit null,
	data_inicio datetime not null,
	local_atividade varchar(255) not null,
	nome_atividade varchar(255) not null,
	pontuacao_participante int not null,
	qtd_checkin int null,
	qtd_vagas int not null,
	id_agendamento int not null,
	constraint FK7b1hphkqj3dkyc2rey2sdbsqe
		foreign key (id_agendamento) references tb_agendamento (id_agendamento)
);

create table tb_inscricao
(
	sq_inscricao int auto_increment
		primary key,
	check_in bit null,
	check_out bit null,
	data_inscricao datetime not null,
	pontuacao_participante int null,
	id_atividade int not null,
	id_papel int not null,
	id_usuario int not null,
	constraint UKjhmy930anf89eimt2ovsqekc1
		unique (id_atividade, id_usuario),
	constraint FK6fsol1303ca17nfmgfxkjbuds
		foreign key (id_atividade) references tb_atividade (id_atividade),
	constraint FKdeoqd96dploi72xmyb4g9c21y
		foreign key (id_papel) references tb_papel (id_papel),
	constraint FKj79h1mrk3yd55vjfkb8jj1dkn
		foreign key (id_usuario) references tb_usuario (id_usuario)
);

create table tb_staff_agendamento
(
	sq_staff int auto_increment
		primary key,
	check_in bit default b'0' not null,
	check_out bit default b'0' not null,
	data_inscricao datetime not null,
	pontuacao_participante int null,
	id_agendamento int not null,
	id_usuario int not null,
	constraint UKs38ghd3jmqelpknr5vqbf6aac
		unique (id_agendamento, id_usuario),
	constraint FK65agl6rppxmkb8jf7yslleaax
		foreign key (id_agendamento) references tb_agendamento (id_agendamento),
	constraint FKom0dawmsb3gfen5ep69w0x5py
		foreign key (id_usuario) references tb_usuario (id_usuario)
);

create table tb_usuario_disciplina
(
	data_inicio_disciplina date not null,
	data_fim_disciplina date null,
	id_disciplina int not null,
	id_usuario int not null,
	primary key (id_disciplina, data_inicio_disciplina, id_usuario),
	constraint FKh8bemqkwkknri9p3vg80fksqe
		foreign key (id_usuario) references tb_usuario (id_usuario),
	constraint FKs0kxm8affjukltq5veefc7nc1
		foreign key (id_disciplina) references tb_disciplina (id_disciplina)
);