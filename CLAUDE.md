# DoceFlux — Sistema de Gestão para Confeitarias

## 1. Contexto do Projeto

O DoceFlux é uma aplicação web mobile-first destinada à gestão operacional e financeira de pequenos negócios de confeitaria.

O objetivo é centralizar o gerenciamento de clientes, produtos, pedidos, encomendas personalizadas, pagamentos e despesas em uma única plataforma.

O sistema deve priorizar simplicidade, confiabilidade e facilidade de utilização, especialmente em dispositivos móveis.

O projeto está em desenvolvimento como um MVP, com validação prevista junto a uma pequena empreendedora do ramo de confeitaria.

**Princípio fundamental:** desenvolver soluções simples para problemas reais, evitando complexidade desnecessária.

## 2. Stack Tecnológica

- **Backend:** Java 25, Spring Boot 4.1.2
- **Persistência:** Spring Data JPA / Hibernate.
- **Banco de dados:** PostgreSQL.
- **Migrations:** Flyway.
- **Build:** Maven.
- **Infraestrutura local:** Docker e Docker Compose.
- **Frontend planejado:** React + TypeScript.

Utilize as versões efetivamente configuradas no `pom.xml` como referência. Não atualize dependências ou versões por iniciativa própria.

Não introduza novas bibliotecas sem necessidade técnica justificada.

## 3. Comandos de Desenvolvimento

### Infraestrutura

Subir os serviços:

`docker compose up -d`

Verificar os containers:

`docker compose ps`

Consultar logs:

`docker compose logs -f`

Parar os serviços:

`docker compose down`

### Backend

Executar a aplicação:

`./mvnw spring-boot:run`

Executar os testes:

`./mvnw test`

Gerar o build:

`./mvnw clean verify`

No Windows, os comandos podem ser executados pelo wrapper `mvnw.cmd`, conforme o terminal utilizado.

## 4. Arquitetura e Organização

Adotar arquitetura de monólito modular, organizada prioritariamente por domínio de negócio.

Os módulos devem possuir responsabilidades bem definidas e baixo acoplamento.

Estrutura de referência:

- `customer`: gerenciamento de clientes.
- `product`: catálogo de produtos.
- `order`: pedidos e encomendas.
- `finance`: pagamentos e despesas.
- `dashboard`: indicadores e consultas.
- `shared`: componentes compartilhados realmente necessários.

Dentro dos módulos, utilizar separação de responsabilidades entre:

- `controller`: exposição dos endpoints HTTP.
- `service`: regras e operações de negócio.
- `repository`: acesso e persistência de dados.
- `domain`: entidades e conceitos do domínio.

Não criar abstrações, interfaces ou camadas adicionais sem justificativa.

Evitar dependências desnecessárias entre módulos.

## 5. Padrões de Código

- Seguir convenções e boas práticas do Java.
- Utilizar nomes claros e expressivos.
- Preferir código simples, legível e de fácil manutenção.
- Aplicar princípios SOLID quando fizer sentido.
- Evitar duplicação de código.
- Evitar métodos excessivamente grandes.
- Não utilizar comentários para explicar código que deveria ser autoexplicativo.
- Não adicionar complexidade antecipando necessidades futuras.

### API

- Utilizar DTOs para entrada e saída dos endpoints.
- Não expor entidades JPA diretamente na API.
- Aplicar Bean Validation nos dados de entrada.
- Centralizar o tratamento de exceções.
- Utilizar códigos HTTP adequados.
- Manter convenções consistentes de nomenclatura e rotas.

### Valores Monetários

- Utilizar obrigatoriamente `BigDecimal` para valores monetários.
- Nunca utilizar `float` ou `double` para representar dinheiro.
- Definir critérios explícitos para arredondamento e escala decimal.
- Preservar os valores históricos dos itens dos pedidos por meio de snapshots de preço e custo.

## 6. Banco de Dados e Migrations

Todas as alterações estruturais no banco de dados devem ser realizadas por migrations do Flyway.

Localização:

`src/main/resources/db/migration`

Regras:

- Utilizar nomenclatura sequencial, por exemplo `V1__create_customer_table.sql`.
- Não alterar migrations já aplicadas.
- Criar novas migrations para modificações posteriores.
- Não utilizar `ddl-auto=update`.
- Preferir validação do schema pela aplicação.
- Respeitar relacionamentos e integridade referencial.
- Não executar operações destrutivas sem autorização explícita.

## 7. Testes e Qualidade

Toda funcionalidade nova deve possuir testes adequados à sua complexidade.

Priorizar:

- Testes unitários para regras de negócio.
- Testes de integração para persistência e comunicação entre componentes.
- Testes de API para endpoints e validações relevantes.

Não considerar uma tarefa concluída sem executar os testes pertinentes.

Não modificar testes apenas para fazer uma implementação incorreta passar.

Não perseguir cobertura de código artificialmente. Priorizar testes que protejam comportamentos importantes.

## 8. Segurança

- Nunca versionar credenciais, tokens ou senhas.
- Utilizar variáveis de ambiente para configurações sensíveis.
- Não expor informações sensíveis em logs.
- Validar e tratar entradas externas.
- Não implementar autenticação ou autorização improvisadamente.
- Considerar privacidade e proteção de dados desde o início.

## 9. Git e Commits

Seguir Conventional Commits.

Exemplos:

- `feat: implement customer registration`
- `fix: correct order payment balance`
- `test: add product service tests`
- `refactor: simplify order validation`
- `docs: update development instructions`
- `chore: configure flyway`

Utilizar branches curtas e relacionadas às tarefas do Jira.

Exemplo:

`feat/DSF-12-create-customer`

Não realizar commits diretamente na branch principal sem necessidade justificada.

## 10. Diretrizes para o Agente de IA

Antes de modificar qualquer código:

1. Inspecione a estrutura existente do projeto.
2. Leia os arquivos relevantes para a tarefa.
3. Identifique os padrões já adotados.
4. Verifique se a implementação respeita a arquitetura existente.
5. Apresente um plano curto quando a tarefa envolver alterações relevantes.

Durante a implementação:

- Trabalhe somente no escopo solicitado.
- Não reescreva arquivos ou módulos sem necessidade.
- Não remova funcionalidades existentes sem autorização.
- Não introduza dependências desnecessárias.
- Não invente requisitos ou regras de negócio.
- Quando houver ambiguidade relevante, pergunte antes de assumir.
- Explique decisões técnicas importantes.

Após a implementação:

1. Execute os testes pertinentes.
2. Verifique possíveis regressões.
3. Informe os arquivos modificados.
4. Explique o que foi implementado.
5. Informe os testes executados e seus resultados.
6. Identifique limitações ou pendências conhecidas.

**Importante:** não considere uma funcionalidade implementada apenas porque o código foi gerado. A implementação precisa ser validada.

## 11. Controle de Escopo

O DoceFlux está sendo desenvolvido inicialmente como um MVP.

Não implementar por iniciativa própria:

- Integração com iFood.
- Integração bancária.
- Integração com Pix.
- Emissão de notas fiscais.
- Automação de WhatsApp.
- Gestão avançada de estoque.
- Inteligência artificial integrada.

Essas funcionalidades pertencem a possíveis versões futuras e dependem de decisões explícitas de produto.

Priorize sempre as tarefas e os critérios de aceite registrados no Jira.