📸 PhotoTravel - Travel with Memories

PhotoTravel é uma aplicação Android nativa desenvolvida para ajudar viajantes a organizar as suas memórias. A aplicação combina fotografia, geolocalização e sincronização na nuvem para criar álbuns automáticos e permitir reviver viagens através de um mapa interativo.

Desenvolvido no âmbito da Unidade Curricular de Desenvolvimento de Aplicações Móveis (Licenciatura em Eng. Informática - IPT).
📱 Funcionalidades Principais
🔐 Autenticação e Nuvem

    Login & Registo: Sistema seguro de autenticação com validação de e-mail e password.

    Sincronização Automática: Arquitetura Offline-First. As fotos tiradas sem internet são guardadas localmente e sincronizadas com o servidor (API REST) assim que a conexão é restabelecida.

📷 Câmara Inteligente (CameraX)

    Captura com GPS: A câmara integrada captura a localização exata (Latitude/Longitude) no momento da foto.

    Gestão Automática: As fotos são automaticamente associadas a coleções.

📂 Gestão de Coleções

    Organização por Álbuns: Visualização em grelha de todas as viagens.

    Renomeação Inteligente (Geocoding): Funcionalidade exclusiva que sugere o nome do álbum com base na localização GPS das fotos (ex: renomeia automaticamente para "Paris" ou "Lisboa").

    Gestão Total: Criar, apagar e renomear coleções, com reflexo imediato no servidor.

🌍 Mapa Interativo

    Mapa de Memórias: Visualização das fotos em pinos no mapa (Google Maps/MapLibre), permitindo ver exatamente onde cada memória foi capturada.

ℹ️ Sobre e Créditos

    Ecrã dedicado com informações dos autores, curso e tecnologias utilizadas.

🛠️ Stack Tecnológica
Android (Cliente)

    Linguagem: Kotlin

    Arquitetura: MVVM (Model-View-ViewModel) com LiveData

    Comunicação API: Retrofit 2 + OkHttp + Gson

    Imagens: Coil (Carregamento assíncrono e caching)

    Hardware:

        CameraX: Gestão avançada da câmara.

        Google Location Services (FusedLocation): Obtenção de coordenadas GPS.

        Geocoder: Conversão de coordenadas em nomes de locais.

    Armazenamento: SharedPreferences (Token) + Armazenamento Interno (Ficheiros).

    Assincronismo: Kotlin Coroutines.

Backend (Servidor)

    Runtime: Node.js

    Framework: Express.js

    Autenticação: JWT (JSON Web Tokens) + Bcrypt.js (Hashing de passwords)

    File System: Base de dados simulada em ficheiros JSON/TXT (NoSQL approach).
