# DoceFlux

**Sistema interno de gestão para pequenas empresas de confeitaria.**

O DoceFlux é uma aplicação desenvolvida para centralizar e organizar as operações administrativas e financeiras de uma empresa real de confeitaria. O objetivo é substituir controles dispersos, como planilhas e anotações manuais, por um sistema simples, organizado e de fácil utilização.

A plataforma permite estruturar informações sobre clientes, produtos, vendas, pedidos e movimentações financeiras, oferecendo uma visão mais clara da operação e auxiliando na tomada de decisões.

> **Importante:** O DoceFlux não é um e-commerce. As vendas são registradas manualmente pela própria empresa, e o catálogo de produtos funciona como referência para esses registros.

## Objetivos do projeto

- Centralizar as informações operacionais e administrativas.
- Organizar o cadastro de clientes e produtos.
- Facilitar o registro e acompanhamento de vendas.
- Organizar pedidos e encomendas.
- Acompanhar recebimentos e despesas.
- Disponibilizar indicadores gerenciais por meio de um dashboard.
- Reduzir a dependência de planilhas e controles manuais dispersos.

## Funcionalidades planejadas

O desenvolvimento está organizado em módulos independentes, seguindo uma arquitetura modular por domínio.

| Módulo | Responsabilidade |
|---|---|
| Customers | Cadastro e gerenciamento de clientes. |
| Products | Catálogo interno de produtos da confeitaria. |
| Orders | Organização e acompanhamento de pedidos e encomendas. |
| Finance | Registro e acompanhamento de recebimentos e despesas. |
| Dashboard | Indicadores e informações gerenciais do negócio. |

*Os módulos representam o escopo planejado. A disponibilidade de cada funcionalidade depende do andamento da implementação.*

## Tecnologias

### Backend
- Java 25
- Spring Boot 4.1.2
- Spring Data JPA
- Hibernate
- Maven
- Springdoc OpenAPI / Swagger

### Banco de dados e infraestrutura
- PostgreSQL
- Flyway
- Docker
- Docker Compose

### Frontend
- React
- TypeScript

## Arquitetura

O backend utiliza uma arquitetura de **monólito modular**, organizado por domínios de negócio.

Cada módulo possui responsabilidades bem definidas, favorecendo a separação de interesses, a manutenção e a evolução das funcionalidades.

## Padrões de desenvolvimento

O projeto adota práticas voltadas à organização, integridade e manutenção do código:

- DTOs para comunicação entre API e clientes.
- Java Records quando apropriado.
- Bean Validation para validação de entradas.
- Tratamento centralizado de exceções.
- Separação entre controllers, services, repositories e entidades.
- BigDecimal para operações monetárias.
- Flyway para versionamento do banco de dados.
- Testes unitários e de integração.
- Variáveis de ambiente para configurações sensíveis.

## Executando localmente

### Pré-requisitos

- Java 25
- Docker e Docker Compose
- Maven (ou Maven Wrapper incluído no projeto)

### 1. Clone o repositório

```bash
git clone https://github.com/lleonalves/doceflux.git
cd doceflux
```

### 2. Inicie a infraestrutura

```bash
docker compose up -d
```

### 3. Execute a aplicação

No Windows:

```bash
mvnw.cmd spring-boot:run
```

No Linux/macOS:

```bash
./mvnw spring-boot:run
```

*As configurações de ambiente e banco de dados devem estar definidas conforme o arquivo de configuração do projeto.*

### 4. Execute os testes

No Windows:

```bash
mvnw.cmd clean verify
```

No Linux/macOS:

```bash
./mvnw clean verify
```

## Documentação da API

A API utiliza Springdoc OpenAPI para documentação dos endpoints.

Quando a aplicação estiver em execução e a documentação estiver configurada, o Swagger UI estará disponível em:

```text
http://localhost:8080/swagger-ui/index.html
```

## Status do projeto

O DoceFlux está em desenvolvimento.

A implementação é realizada de forma incremental, priorizando os módulos essenciais para a operação real da empresa e respeitando as necessidades identificadas durante o desenvolvimento.

## Autor

**Leon Alves Martins**

Estudante de Engenharia de Software e desenvolvedor backend em formação, com foco em Java, Spring Boot e desenvolvimento de APIs REST.

- GitHub: [@lleonalves](https://github.com/lleonalves)
- LinkedIn: [Leon Alves Martins](https://www.linkedin.com/in/leon-alves-martins/)