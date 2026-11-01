# DoceFlux — Sistema Interno de Gestão para Confeitarias

## 1. Contexto e Propósito do Projeto

O **DoceFlux** é uma aplicação web de gestão desenvolvida para atender às necessidades operacionais e administrativas de uma empresa real do ramo de confeitaria.

O sistema tem como objetivo centralizar informações do negócio em um único ambiente, facilitando a organização de clientes, produtos, vendas, pagamentos e despesas.

A aplicação será utilizada internamente pela própria empresa, funcionando como um dashboard administrativo para registro de informações e acompanhamento gerencial.

### Modelo de operação

A empresa será responsável por alimentar o sistema manualmente, registrando suas operações e mantendo as informações atualizadas.

O DoceFlux não realizará comercialização de produtos diretamente aos consumidores.

**Princípio fundamental:** desenvolver uma ferramenta simples, confiável e funcional que resolva problemas reais de gestão, sem adicionar complexidade ou funcionalidades que não atendam às necessidades da empresa.

### Objetivos do produto

- Centralizar informações administrativas e operacionais.
- Organizar o cadastro de clientes e produtos.
- Permitir o registro manual de vendas.
- Facilitar o acompanhamento de pagamentos e despesas.
- Disponibilizar indicadores gerenciais por meio de um dashboard.
- Reduzir a dependência de cadernos, planilhas e controles dispersos.
- Melhorar a organização e a visibilidade financeira do negócio.

O projeto está sendo desenvolvido como um MVP, com validação contínua junto à empresa atendida.

## 2. Regras de Negócio Fundamentais

As seguintes regras devem orientar todas as decisões de arquitetura e implementação.

### 2.1. Operação interna

- O sistema será utilizado exclusivamente para gestão interna da empresa.
- Os registros serão realizados manualmente pelos usuários responsáveis.
- O sistema não será disponibilizado como loja virtual ou canal de comercialização.
- O catálogo de produtos será utilizado como referência interna.
- O dashboard deverá apresentar informações provenientes dos registros realizados no próprio sistema.

### 2.2. Registro de vendas

- As vendas serão registradas manualmente pela empresa.
- O sistema deverá permitir organizar e consultar as vendas realizadas.
- Os registros deverão preservar informações históricas relevantes.
- Os valores registrados deverão permitir futuras consultas e análises gerenciais.
- O registro de uma venda não deverá depender de checkout, gateway ou confirmação de pagamento externa.

### 2.3. Gestão financeira

- Pagamentos e recebimentos serão registrados internamente.
- Despesas poderão ser organizadas para acompanhamento financeiro.
- O sistema deverá permitir visualizar informações financeiras a partir dos registros realizados.
- Não presumir integração bancária ou conciliação automática.
- Não implementar processamento financeiro externo sem decisão explícita de produto.

### 2.4. Catálogo de produtos

- Os produtos serão cadastrados internamente pela empresa.
- O catálogo servirá de referência para os futuros registros de vendas.
- Produtos poderão ser marcados como ativos ou inativos.
- A alteração do preço atual de um produto não deverá modificar valores históricos de vendas já registradas.
- Não implementar estoque avançado sem necessidade validada.

### 2.5. Pedidos e encomendas

Pedidos e encomendas poderão ser organizados como informações operacionais internas, conforme as necessidades identificadas junto à empresa.

Não presumir que todo pedido representa uma venda concluída ou que toda venda exige um fluxo de encomenda.

As regras específicas de relacionamento entre pedidos, vendas e pagamentos deverão ser definidas nas respectivas tarefas de implementação.

### 2.6. Funcionalidades fora do modelo de negócio

Não implementar por iniciativa própria:

- E-commerce ou loja virtual.
- Marketplace.
- Checkout online.
- Carrinho de compras.
- Processamento de pagamentos.
- Integração com delivery.
- Portal de clientes para compras.
- Comercialização automatizada de produtos.

Qualquer mudança nesse modelo depende de decisão explícita de produto.

## 3. Stack Tecnológica

### Backend

- **Linguagem:** Java 25.
- **Framework:** Spring Boot 4.1.2.
- **Persistência:** Spring Data JPA / Hibernate.
- **Banco de dados:** PostgreSQL.
- **Migrations:** Flyway.
- **Build:** Maven.
- **Documentação da API:** Springdoc OpenAPI / Swagger.

### Infraestrutura

- Docker.
- Docker Compose.

### Frontend planejado

- React.
- TypeScript.

O frontend deverá priorizar uma experiência mobile-first, com interface simples e adequada à rotina operacional da empresa.

**Regras de dependências:**

- Utilizar as versões efetivamente configuradas no `pom.xml` como referência.
- Não atualizar dependências ou versões por iniciativa própria.
- Não introduzir novas bibliotecas sem necessidade técnica justificada.
- Antes de adicionar uma dependência, verificar se a funcionalidade já pode ser atendida pelas tecnologias existentes.

## 4. Comandos de Desenvolvimento

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

No Windows, utilizar `mvnw.cmd`, conforme o terminal utilizado.

## 5. Arquitetura e Organização

Adotar arquitetura de **monólito modular**, organizada prioritariamente por domínio de negócio.

Os módulos devem possuir responsabilidades bem definidas, baixo acoplamento e independência sempre que possível.

### Estrutura de referência

- `customer`: cadastro e gerenciamento de clientes.
- `product`: cadastro e gerenciamento do catálogo interno de produtos.
- `order`: gerenciamento de pedidos e encomendas.
- `finance`: pagamentos, recebimentos e despesas.
- `dashboard`: indicadores e consultas gerenciais.
- `shared`: componentes compartilhados realmente necessários.

A estrutura representa a organização conceitual do sistema. A implementação de cada módulo deverá respeitar as tarefas e prioridades definidas no Jira.

### Organização interna dos módulos

Utilizar separação de responsabilidades entre:

- `controller`: exposição dos endpoints HTTP.
- `service`: regras e operações de negócio.
- `repository`: acesso e persistência de dados.
- `domain`: entidades e conceitos do domínio.
- `dto`: objetos de transferência de dados, quando aplicável.

Não criar abstrações, interfaces ou camadas adicionais sem justificativa técnica.

### Regras arquiteturais

- Evitar dependências desnecessárias entre módulos.
- Não permitir que controllers concentrem regras de negócio.
- Não expor entidades JPA diretamente na API.
- Centralizar regras de negócio na camada de serviço.
- Reutilizar componentes compartilhados somente quando houver necessidade real.
- Evitar acoplamento entre módulos por meio de acesso direto às estruturas internas uns dos outros.
- Preservar a simplicidade arquitetural adequada ao MVP.

## 6. Padrões de Código

- Seguir convenções e boas práticas do Java.
- Utilizar nomes claros e expressivos.
- Preferir código simples, legível e de fácil manutenção.
- Aplicar princípios SOLID quando fizer sentido.
- Evitar duplicação de código.
- Evitar métodos excessivamente grandes.
- Não utilizar comentários para explicar código que deveria ser autoexplicativo.
- Não adicionar complexidade antecipando necessidades futuras.
- Manter consistência com os padrões já estabelecidos no projeto.

### API REST

- Utilizar DTOs para entrada e saída dos endpoints.
- Preferir Java Records para DTOs quando apropriado.
- Não expor entidades JPA diretamente.
- Aplicar Bean Validation nos dados de entrada.
- Utilizar `@Valid` nos endpoints que recebem dados sujeitos à validação.
- Centralizar o tratamento de exceções.
- Utilizar códigos HTTP adequados.
- Manter convenções consistentes de nomenclatura e rotas.
- Documentar os endpoints utilizando Swagger/OpenAPI.
- Não implementar regras de negócio diretamente nos controllers.

### Valores monetários

- Utilizar obrigatoriamente `java.math.BigDecimal` para valores monetários.
- Nunca utilizar `float` ou `double` para representar dinheiro.
- Utilizar tipos DECIMAL ou NUMERIC no banco de dados.
- Definir critérios explícitos para precisão, escala e arredondamento.
- Validar valores monetários na entrada da API.
- Evitar conversões que provoquem perda de precisão.

### Histórico financeiro e comercial

Quando uma venda possuir itens relacionados a produtos cadastrados, preservar os valores praticados no momento da operação.

Os registros históricos deverão manter snapshots dos valores relevantes, especialmente:

- Nome ou identificação do produto no momento da venda.
- Quantidade comercializada.
- Preço unitário praticado.
- Valor total do item.

Caso o sistema passe a controlar custos, os respectivos valores históricos também deverão ser preservados.

Alterações posteriores no catálogo não deverão modificar registros históricos.

## 7. Banco de Dados e Migrations

Todas as alterações estruturais no banco de dados deverão ser realizadas por migrations do Flyway.

Localização:

`src/main/resources/db/migration`

### Regras

- Utilizar nomenclatura sequencial, por exemplo `V1__create_customer_table.sql`.
- Verificar a última versão existente antes de criar uma nova migration.
- Não sobrescrever migrations existentes.
- Não alterar migrations já aplicadas.
- Criar novas migrations para modificações posteriores.
- Não utilizar `ddl-auto=update`.
- Preferir validação do schema pela aplicação.
- Respeitar relacionamentos e integridade referencial.
- Não executar operações destrutivas sem autorização explícita.
- Garantir compatibilidade entre entidades JPA e estrutura do banco.
- Utilizar tipos adequados para cada informação persistida.

## 8. Testes e Qualidade

Toda funcionalidade nova deverá possuir testes adequados à sua complexidade.

Priorizar:

- Testes unitários para regras de negócio.
- Testes de integração para persistência e comunicação entre componentes.
- Testes de API para endpoints e validações relevantes.

### Diretrizes

- Seguir a estratégia de testes já adotada no projeto.
- Não introduzir novas tecnologias de teste sem necessidade justificada.
- Testar cenários válidos e inválidos.
- Verificar respostas HTTP.
- Verificar persistência e integridade dos dados.
- Garantir que alterações em um módulo não provoquem regressões nos demais.
- Não modificar testes apenas para fazer uma implementação incorreta passar.
- Não perseguir cobertura de código artificialmente.
- Priorizar testes que protejam comportamentos importantes.

Não considerar uma tarefa concluída sem executar os testes pertinentes.

## 9. Segurança e Proteção de Dados

- Nunca versionar credenciais, tokens ou senhas.
- Utilizar variáveis de ambiente para configurações sensíveis.
- Não expor informações sensíveis em logs.
- Validar e tratar entradas externas.
- Não implementar autenticação ou autorização improvisadamente.
- Respeitar os princípios de privacidade e proteção de dados.
- Evitar exposição desnecessária de informações pessoais de clientes.
- Considerar a LGPD desde o início do desenvolvimento.

## 10. Git e Commits

Seguir Conventional Commits.

Exemplos:

- `feat: implement customer registration`
- `feat: add product catalog`
- `fix: correct order payment balance`
- `test: add product service tests`
- `refactor: simplify order validation`
- `docs: update development instructions`
- `chore: configure flyway`

Utilizar branches curtas e relacionadas às tarefas do Jira.

Exemplo:

`feat/DSF-12-create-customer`

Não realizar commits diretamente na branch principal sem necessidade justificada.

Não realizar commits automaticamente sem autorização explícita.

## 11. Diretrizes para o Agente de IA

O agente de IA deverá atuar como um assistente de desenvolvimento, respeitando as decisões arquiteturais, técnicas e de produto estabelecidas neste documento.

### Antes de modificar qualquer código

1. Leia este arquivo integralmente.
2. Inspecione a estrutura existente do projeto.
3. Leia os arquivos relevantes para a tarefa.
4. Identifique os padrões já adotados.
5. Verifique as dependências e configurações existentes.
6. Confirme que a implementação respeita as regras de negócio.
7. Identifique possíveis impactos em outros módulos.
8. Apresente um plano curto quando a tarefa envolver alterações relevantes.

### Durante a implementação

- Trabalhe somente no escopo solicitado.
- Não reescreva arquivos ou módulos sem necessidade.
- Não remova funcionalidades existentes sem autorização.
- Não introduza dependências desnecessárias.
- Não invente requisitos ou regras de negócio.
- Não altere decisões arquiteturais por iniciativa própria.
- Quando houver ambiguidade relevante, pergunte antes de assumir.
- Não antecipe funcionalidades de tarefas futuras.
- Explique decisões técnicas importantes.
- Preserve a compatibilidade com os módulos existentes.

### Respeito às regras de negócio

Antes de implementar qualquer funcionalidade, verifique se ela atende ao modelo operacional do DoceFlux.

Não transforme funcionalidades internas de gestão em funcionalidades de comercialização online.

Não presuma integrações, automações ou fluxos operacionais que não tenham sido explicitamente definidos.

Quando uma necessidade de negócio não estiver suficientemente especificada, solicite esclarecimentos antes de implementar.

### Após a implementação

1. Execute os testes pertinentes.
2. Execute o build quando aplicável.
3. Verifique possíveis regressões.
4. Informe os arquivos criados e modificados.
5. Explique o que foi implementado.
6. Informe os testes executados e seus resultados.
7. Identifique limitações ou pendências conhecidas.
8. Informe qualquer desvio em relação ao escopo solicitado.

**Importante:** não considere uma funcionalidade implementada apenas porque o código foi gerado. A implementação precisa ser validada.

## 12. Controle de Escopo e Evolução do MVP

O DoceFlux está sendo desenvolvido inicialmente como um MVP para atender às necessidades reais de uma pequena empresa de confeitaria.

O desenvolvimento deverá priorizar funcionalidades essenciais para a gestão cotidiana.

Não implementar por iniciativa própria:

- E-commerce ou loja virtual.
- Integração com iFood.
- Integração bancária.
- Integração com Pix.
- Processamento de pagamentos externos.
- Emissão de notas fiscais.
- Automação de WhatsApp.
- Gestão avançada de estoque.
- Inteligência artificial integrada.
- Integrações com plataformas externas.
- Funcionalidades não previstas nas tarefas aprovadas.

Essas funcionalidades pertencem a possíveis evoluções futuras e dependem de validação das necessidades da empresa e decisão explícita de produto.

### Prioridades de desenvolvimento

1. Construir uma base técnica confiável.
2. Implementar os módulos essenciais de gestão.
3. Garantir consistência e integridade dos dados.
4. Facilitar o registro e a consulta das operações.
5. Desenvolver indicadores gerenciais úteis.
6. Validar continuamente as funcionalidades com a empresa.

Priorize sempre as tarefas e os critérios de aceite registrados no Jira.

**Princípio final:** o DoceFlux deve evoluir conforme os problemas reais da empresa, não conforme funcionalidades que parecem interessantes de implementar.