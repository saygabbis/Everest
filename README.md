# Everest

Rede de Minecraft focada no Brasil, no estilo Hypixel: minigames, PvP 1.8 e survival no mesmo lugar.

Jogadores entram pelo proxy e escolhem o modo no lobby. Clientes de **1.8 até 1.21**. O combate dos minigames é **1.8.8**.

## Modos

- **Lobby** — hub da rede
- **Duels** — 1v1
- **Bedwars**
- **Survival**
- Outros modos depois (SkyWars, The Bridge, etc.)

## Como a rede se organiza

```
Jogador (1.8 → 1.21)
        ↓
   Velocity (proxy)
        ├── Lobby
        ├── Duels
        ├── Bedwars
        └── Survival
```

Cada modo roda em servidor próprio. O que é comum à rede (conta, coins, party, troca de servidor) fica no **Core**. Os jogos são plugins separados.

## Stack

| Peça | Escolha |
| --- | --- |
| Proxy | Velocity |
| Versões de cliente | ViaVersion + ViaBackwards + ViaRewind |
| Minigames / lobby | PandaSpigot 1.8.8 |
| Dados | MariaDB + Redis |
| Linguagem | Java 17 |

## Este repositório

Um monorepo. Os módulos (`api`, `core`, `lobby`, `duels`, …) entram conforme a rede for sendo construída.

Prioridade atual: a rede existir (Core + proxy + lobby). Os modos vêm depois.
