# L1-Tube · Lista lateral

Padrão visual aprovado para a evolução do L1-Tube em Android TV. O menu HQ durante a reprodução é a primeira aplicação. Use a mesma linguagem visual em configurações, diálogos, seletores, listas e painéis de informação. Cada tela conserva a navegação adequada à sua função.

## Paleta

| Uso | Cor | Recurso Android |
| --- | --- | --- |
| Fundo do painel | `#131925` | `l1_menu_surface` |
| Fundo da opção | `#1B2432` | `l1_menu_row_surface` |
| Borda discreta do painel | `#344257` | `l1_menu_border` |
| Fundo de foco e pressão | `#3D2933` | `l1_menu_focus_surface` |
| Contorno de foco e destaque | `#FFB7B2` | `l1_focus` |
| Texto principal | `#F4F6FC` | `l1_text_primary` |
| Texto secundário | `#BCC7D9` | `l1_text_secondary` |

Os recursos ficam em `smarttubetv/src/main/res/values/l1_theme.xml`. Reutilize os recursos pelo seu papel na interface. Reserve o coral para foco, confirmação e outros destaques pontuais.

## Identidade no launcher

- O nome exibido é **L1-Tube**.
- A marca é um triângulo de reprodução azul em relevo com o monograma **L1** branco dentro dele. Deixe margem ao redor de todo o triângulo, inclusive nos vértices do lado esquerdo, para o recorte circular do launcher.
- O banner da TV usa a mesma marca sobre a superfície escura do tema e o prefixo **L1** em azul.

## Forma e tipografia

- Use superfícies escuras opacas, borda discreta de 1 dp e raio de 16 dp em painéis. Use raio de 8 dp em linhas e controles de menu.
- Use uma escala de 4, 8, 16 e 24 dp. O menu HQ usa 4 dp entre linhas, 8 dp de margem interna no painel e 16 dp nas laterais da linha. As categorias têm pelo menos 48 dp de altura e crescem quando o texto precisa quebrar.
- Use a fonte nativa sem serifa. O cabeçalho identifica a tela, os títulos das opções usam peso médio e os detalhes ficam abaixo em cor secundária. Mantenha o texto legível com a fonte ampliada do Android.
- A lista de vídeo mantém resolução e HDR juntos, como **(4K) 2160p • HDR10/PQ**, com **AV1 · 24 fps · 26,2 Mbps** abaixo. A lista de áudio usa o nome completo, como **Dolby Digital Plus**, com **5.1 surround · 384 kbps** abaixo quando esses dados estiverem disponíveis.
- O cabeçalho da tela inicial reserva o canto superior direito à data e à hora em 24 horas.
- No player, a data e a hora aparecem no canto superior esquerdo. O bloco no lado direito, abaixo da barra de progresso, mostra os dados das faixas ativas em linhas de **Vídeo** e **Áudio**. Use abreviações legíveis, como **DD+**, nesse espaço compacto.

## Controle remoto e estados

- Setas para cima e para baixo percorrem a lista. **OK** abre uma categoria ou aplica uma opção. **Voltar** fecha o submenu e devolve o foco à categoria de origem.
- A linha focada usa contorno coral de 2 dp. O rádio ou a caixa de seleção indica o valor aplicado independentemente do foco. A seta no fim de uma linha indica que ela abre um submenu.
- Deixe o vídeo visível enquanto o painel lateral estiver aberto. Preserve o botão **HQ** na fileira atual do player e mantenha as opções que aparecem conforme o dispositivo ou a reprodução.
- Use a ordem atual das categorias, com **Predefinições de áudio** logo abaixo de **Predefinições de vídeo**. O menu de formatos escolhe a faixa do vídeo atual. As predefinições guardam uma preferência.

## Aplicação nas próximas telas

1. Reuse a paleta, os espaçamentos e os estados nativos de foco. Ajuste o layout à função da tela.
2. Confira navegação com o controle remoto, legibilidade à distância, rolagem, textos longos e retorno de foco.
3. Para uma mudança grande de layout, mostre pelo menos duas prévias que partam da interface atual e aguarde a escolha antes de aplicar.
