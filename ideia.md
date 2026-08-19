# Ideias da Everest

Documento **solto**, só pra organizar o que a gente pensou.  
Pode (e deve) ser editado, riscado, contradito. Não é spec, não é contrato, não é arquitetura travada.

Última conversa em mente: Core como **núcleo técnico**, o resto como plugins que se ligam nele.

---

## O que é a Everest

Rede BR estilo Hypixel: minigames, PvP 1.8 e survival no mesmo lugar.

Não é um plugin gigante. É uma **rede** com uma cara só. Cada pasta é um produto interno; o jogador só vê Everest.

Clientes 1.8 → 1.21. Combate dos minigames em **1.8.8**.

```
Jogador
   ↓
Velocity (proxy)
   ├── Lobby
   ├── Parkour
   ├── Duels
   ├── Bedwars
   └── Survival
```

ViaVersion / Backwards / Rewind cuidam de versão. Não misturar isso com Core nem com modo.

---

## As 3 camadas (visão de produto)

```
Marca / comunicação     mensagens, menus, hologramas, sons, nomenclatura
        ↑
Modos e serviços        Parkour, Duels, Lobby, Spawn, Economia, …
        ↑
Núcleo + proxy          dados, API, barramento, troca de servidor
```

O jogador **não vê** o Core. Vê Lobby, Parkour, coins, `/spawn`.  
O Core é o que faz isso parecer um produto só.

---

## Core = núcleo (destino)

Ideia: o Core **não** é um Essentials. É gestor + comunicador.

Faz:

- identidade do jogador (UUID, nome, first/last join)
- persistência (FILE no lab, MySQL/Redis quando for rede de verdade)
- API que todo plugin compila contra (`everest-api`)
- registro de serviços (“quem oferece economia?”, “quem oferece spawn?”)
- eventos / hooks / chamadas entre plugins Everest
- ponte com terceiros (LuckPerms, PlaceholderAPI, Vault, etc. — se um dia existir)
- proxy: `sendToServer`, `server.id`, `ServerType`

Não faz (sai do Core com o tempo):

- spawn, holograma, NPC
- economia / loja / kits
- timer, checkpoint, arena, cama
- menu bonito de jogador
- `/evconfig` de spawn (isso é UI de *serviço*, não de kernel)

Hoje o JAR do Core ainda mistura núcleo + spawn + coins + menu. Ok por enquanto. O desenho alvo é fatiar.

Regra mental: **Parkour não fala com Spawn. Parkour fala com a API. Spawn e Economia se apresentam. O Core encaminha.**

```
EverestParkour ──► EverestAPI ──► Core (núcleo)
                       ▲
         EverestEconomy ┘
         EverestSpawn   ┘
         (terceiros)    ┘  via adaptador
```

Três jeitos de se falar (o núcleo padroniza):

1. **Serviço registrado** — `EverestAPI.service(Algo.class)`
2. **Evento** — tipo o `EverestUserLoadEvent` que já existe
3. **Terceiros** — uma porta só, não cada modo inventar um hook

`EverestUser` no núcleo fica magro: uuid, nome, datas.  
`getCoins()` um dia vira `EverestEconomy.get(player)`.

Modo pode ser `softdepend` no Core (roda isolado no lab).  
Serviço (Spawn, Economia) faz `depend: [EverestCore]` — sem núcleo, não faz sentido.

---

## Pack de plugins (ideia de pastas)

```
EverestCore         núcleo — dependência de todo mundo
EverestEconomy      coins da rede
EverestSpawn        spawns / join / respawn  (ou um Essentials magro)
EverestLobby        fachada / vitrine
EverestParkour      modo
EverestDuels        modo
EverestBedwars      modo
EverestSurvival     modo
EverestProxy        plugin Velocity (motd, queue, /server) — depois
```

Cada servidor, na prática:

| Máquina | JARs |
| --- | --- |
| qualquer backend “de rede” | Core |
| lobby / parkour / duels / … | Core + Spawn (quase sempre) |
| minigames | Core + Economy |
| `parkour-1` | Core + Economy + Spawn + Parkour |
| `lobby-1` | Core + Economy + Spawn + Lobby (+ Via*) |

Não colocar Parkour no lobby. O Lobby **manda** o player pro servidor (`sendToServer(..., "parkour-1")`).

Survival pode ter **economia própria** (money do mundo) além dos coins da rede. O núcleo não mistura as regras; só o UUID é comum.

---

## O que já existe (não jogar fora)

Molde certo, o Parkour já segue:

1. `compileOnly` da **API**, nunca do `core` interno
2. `softdepend: [EverestCore]`
3. `EverestHook` com id estável (`parkour`)
4. Coins só se `EverestAPI.isAvailable()`
5. comando e `messages.yml` **do próprio modo**

Todo modo novo copia isso.  
A API cresce **devagar**. Stats/party entram quando o **segundo** modo precisar, não antes.

`ServerType` no `config.yml` do Core em cada máquina. Ideia extra: plugin de modo recusar ligar no tipo errado (Parkour no survival) pra não ter deploy idiota.

---

## Dúvida “isso vai em qual pasta?”

> O Survival precisa disso **sem** ter Parkour instalado?

- Se sim, e é identidade/dado/comunicação → **Core**
- Se sim, e é “onde nasce / coins / party” → **serviço** (Spawn, Economy, …)
- Se não → **modo**

YAML, scheduler, cores: lib interna, **não** vira plugin.

---

## Não fatiar cedo demais

Núcleo puro é o desenho certo. Cinco JARs no dia 1 é dor de deploy e ordem de load.

Caminho são:

- **Agora:** Core *mentalmente* é núcleo. Spawn e coins continuam módulos (`spawn/`, `user/`). Pode ficar no mesmo JAR.
- **Quando fatiar:** dois consumidores de verdade + o Core inchando.
  - Economia sai quando Lobby (loja) + Parkour (reward) + Survival não couberem num `EverestUser`.
  - Spawn sai quando Lobby quiser spawn “de produto” e o Parkour quiser um spawn burro, sem a GUI de admin no mesmo código.
- **Não fatiar** utilzinho de YAML/cor/scheduler.

---

## Produto comunicativo (a cara)

Mesmo prefixo de chat, mesma paleta, mesmos verbos (*entrar, sair, recorde, coins*).  
Um prefixo de **rede** e um de **modo**, repetidos em todos os `messages.yml`.

- Menu de op (`/evconfig`) pode ser técnico.
- Menu de jogador (Lobby) é vitrine.

Loop mínimo de **rede** (sem isso é plugin isolado):

**Lobby → clica Parkour → servidor parkour → joga → coins no Core → volta ao Lobby.**

`EverestTest/` é lab local, fora do git. Um `config.yml` por servidor com `id` e `type`.

---

## Ordem (rascunho, pode mudar)

1. Fechar o Parkour como modo (pistas, timer, chegada, failheight, recorde).
2. Core “rede mínima”: MYSQL + `sendToServer` no Velocity (Lobby ainda pode ser gambiarra).
3. EverestLobby — seletor, spawn bonito, perfil/coins. É o que vende a rede.
4. Segundo modo (Duels ou Bedwars) — prova que o núcleo aguenta dois filhos.
5. Survival por último.

Não começar SkyWars/Bridge antes do loop Lobby ↔ 1 modo ↔ coins.

---

## Dia a dia

- Pasta = plugin = responsabilidade.
- API pequena. Cada método novo na `EverestAPI` é um casamento.
- Um `messages.yml` por plugin, um tom só (você/tu, pista/mapa, cores).
- Commit por fatia de produto, não por arquivo solto.

---

## Stack (o que já combinamos)

| Peça | Escolha |
| --- | --- |
| Proxy | Velocity |
| Versões | ViaVersion + Backwards + Rewind |
| Minigames / lobby | PandaSpigot 1.8.8 |
| Dados | MariaDB + Redis (FILE no lab) |
| Linguagem | Java 17 |

O que **ainda não está** e provavelmente é núcleo/serviço depois: party, friend, punish/report, rank (LuckPerms ao lado), Redis de presença, perfil/stats agregadas.

---

## Lembrete

Isso aqui é caderno. Se amanhã o Core continuar com spawn dentro, tudo bem. Se Economia virar plugin na semana que vem, também. O importante é não esquecer a direção: **núcleo técnico, o resto se conecta.**
