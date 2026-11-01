# ≡ƒù║∩╕Å Kß║╛ HOß║áCH 4 NG├ÇY (01/11 ΓÇô 04/11/2026): KHO KIß║╛N THß╗¿C ΓåÆ B├üO C├üO S├üCH ΓåÆ Bß╗ÿ KHUNG AI ΓåÆ THIß║╛T Kß║╛ OOA/OOD

> **Quyß║┐t ─æß╗ïnh ng├áy 31/10/2026**: Tr╞░ß╗¢c khi code tiß║┐p 8 hß║íng mß╗Ñc mß╗ƒ rß╗Öng, nh├│m d├ánh **4 ng├áy** (01/11 ΓåÆ 04/11) ─æß╗â dß╗▒ng nß╗ün tri thß╗⌐c v├á thiß║┐t kß║┐. Tß╗½ **05/11** mß╗¢i quay lß║íi code, v├á **code theo ─æ├║ng thiß║┐t kß║┐ ─æ├ú chß╗æt** (kh├┤ng code theo cß║úm t├¡nh nß╗»a).
>
> **Nguy├¬n tß║»c cß╗æt l├╡i**: Khß╗æi l╞░ß╗úng n├áy l├á **h├áng chß╗Ñc prompt**, kh├┤ng thß╗â l├ám trong 1 lß║ºn. Mß╗ùi prompt chß╗ë l├ám **1 viß╗çc nhß╗Å ΓåÆ ra 1 file ΓåÆ ─æ╞░ß╗úc ng╞░ß╗¥i phß╗Ñ tr├ích duyß╗çt ΓåÆ commit**, rß╗ôi mß╗¢i sang prompt kß║┐ tiß║┐p.

---

## 1. Bß╗¿C TRANH Tß╗öNG THß╗é

```mermaid
flowchart LR
    D0["31/10<br>Chß╗æt kß║┐ hoß║ích"] --> D1["Ng├áy 1 ┬╖ 01/11<br>PHA A: Kho kiß║┐n thß╗⌐c<br>5 cuß╗æn s├ích"]
    D1 --> D2["Ng├áy 2 ┬╖ 02/11<br>PHA B: 4 b├ío c├ío t├│m tß║»t"]
    D2 --> D3["Ng├áy 3 ┬╖ 03/11<br>S├íng - PHA C: Bß╗Ö khung AI<br>Chiß╗üu - PHA D1: OOA"]
    D3 --> D4["Ng├áy 4 ┬╖ 04/11<br>S├íng - PHA D2: OOD<br>Chiß╗üu - PHA D3: Design + chß╗æt v1.0"]
    D4 --> D5["05/11 trß╗ƒ ─æi<br>Code 8 hß║íng mß╗Ñc<br>theo thiß║┐t kß║┐ ─æ├ú ─æ├│ng b─âng"]
```

| Pha | Ng├áy | Sß║ún phß║⌐m ─æß║ºu ra | Phß╗Ñ thuß╗Öc v├áo |
| :-: | :-: | :--- | :--- |
| **A** | 01/11 | **Kho kiß║┐n thß╗⌐c** r├ánh mß║ích cho cß║ú nh├│m (mß╗Ñc lß╗Ñc + thß║╗ kiß║┐n thß╗⌐c tß╗½ng ch╞░╞íng cß╗ºa 5 cuß╗æn) | ΓÇö |
| **B** | 02/11 | **4 b├ío c├ío t├│m tß║»t** (Larman ┬╖ Nystrom ┬╖ Glazer & Madhav ┬╖ Fowler + Vernon) | Pha A |
| **C** | 03/11 s├íng | **Bß╗Ö khung thß╗æng nhß║Ñt cho AI** (luß║¡t dß╗▒ ├ín, mß║½u prompt, mß║½u t├ái liß╗çu, quy ╞░ß╗¢c code/test) | Pha A, B |
| **D1** | 03/11 chiß╗üu | **OOA**: danh mß╗Ñc chß╗⌐c n─âng, Use Case, Domain Model, SSD, Contract | Pha C |
| **D2** | 04/11 s├íng | **OOD**: kiß║┐n tr├║c, Sequence, DCD, FSM, giao thß╗⌐c mß║íng, ├ính xß║í GRASP/GoF | D1 |
| **D3** | 04/11 chiß╗üu | **Design chi tiß║┐t**: wireframe, bß║úng sß╗æ liß╗çu, kß║┐ hoß║ích test, ma trß║¡n truy vß║┐t, **─æ├│ng b─âng v1.0** | D2 |

> ≡ƒôÜ **5 cuß╗æn s├ích**: Larman (*Applying UML and Patterns*) ┬╖ Nystrom (*Game Programming Patterns*) ┬╖ Glazer & Madhav (*Multiplayer Game Programming*) ┬╖ Fowler (*PoEAA*) ┬╖ Vernon (*Implementing DDD*).
> **4 b├ío c├ío** v├¼ Fowler + Vernon gß╗Öp chung 1 b├ío c├ío (c├╣ng chß╗º ─æß╗ü kiß║┐n tr├║c doanh nghiß╗çp / miß╗ün nghiß╗çp vß╗Ñ).

---

## 2. QUY Tß║«C CHß║áY PROMPT (─Éß╗é KH├öNG Bß╗è LOß║áN)

1. **1 prompt = 1 viß╗çc = 1 file ─æß║ºu ra.** Kh├┤ng gß╗Öp nhiß╗üu viß╗çc v├áo mß╗Öt prompt.
2. **S├ích d├ái th├¼ chia nhß╗Å**: tß╗æi ─æa **~5 ch╞░╞íng/prompt** (╞░ß╗¢c t├¡nh: Larman 38 ch╞░╞íng Γëê 8 prompt, Nystrom 21 Γëê 5, Glazer & Madhav 14 Γëê 3, Fowler Γëê 6 theo nh├│m pattern, Vernon Γëê 3).
3. **Mß╗ùi prompt ghi r├╡ 4 thß╗⌐**: ─Éß║ºu v├áo (─æß╗ìc file n├áo) ┬╖ ─Éß║ºu ra (ghi file n├áo) ┬╖ R├áng buß╗Öc ┬╖ Ti├¬u ch├¡ xong (DoD).
4. **Cß╗òng chß║Ñt l╞░ß╗úng (Gate)**: ng╞░ß╗¥i phß╗Ñ tr├ích **─æß╗ìc lß║íi v├á duyß╗çt** ─æß║ºu ra tr╞░ß╗¢c khi chß║íy prompt kß║┐ tiß║┐p. AI sinh sai th├¼ sß╗¡a ngay, kh├┤ng dß╗ôn lß║íi.
5. **Kh├┤ng bß╗ïa tr├¡ch dß║½n**: mß╗ìi nß╗Öi dung lß║Ñy tß╗½ s├ích phß║úi gß║»n **m├ú thß║╗ kiß║┐n thß╗⌐c** (vd `LAR-07-02`) hoß║╖c sß╗æ trang; kh├┤ng chß║»c th├¼ ghi `[Cß║ªN KIß╗éM TRA]`.
6. **Commit sau mß╗ùi cß╗Ñm prompt**: `docs(<pha>): <M├â PROMPT> <nß╗Öi dung>` (vd `docs(kb): KT-04 nystrom chapters 1-5`).
7. **Bß║ún quyß╗ün**: file PDF v├á bß║ún b├│c t├ích nguy├¬n v─ân nß║▒m trong `_Archive_CaNhan/` (─æ├ú `.gitignore`, **kh├┤ng ─æß║⌐y l├¬n GitHub**). Chß╗ë commit **thß║╗ kiß║┐n thß╗⌐c do nh├│m tß╗▒ t├│m tß║»t**, kh├┤ng d├ín nguy├¬n v─ân s├ích.
8. **Chß║íy song song**: c├íc prompt cß╗ºa tß╗½ng ng╞░ß╗¥i (mß╗ùi ng╞░ß╗¥i mß╗Öt cuß╗æn / mß╗Öt cß╗Ñm UC) chß║íy song song ─æ╞░ß╗úc; c├íc prompt "chß╗æt" (KT-09, BC-08, AI-08, DES-05) chß╗ë chß║íy khi mß╗ìi phß║ºn tr╞░ß╗¢c ─æ├ú duyß╗çt.

**Mß║½u prompt chuß║⌐n** (sß║╜ ─æ╞░ß╗úc chß╗æt ch├¡nh thß╗⌐c ß╗ƒ AI-03):

```text
VAI TR├Æ: <AI ─æ├│ng vai g├¼>
Bß╗ÉI Cß║óNH: <─æß╗ìc .agent/rules/project-guide.md + file n├áo>
NHIß╗åM Vß╗ñ: <─æ├║ng 1 viß╗çc>
R├ÇNG BUß╗ÿC: <kh├┤ng bß╗ïa trang, theo template n├áo, kh├┤ng ─æß╗Ñng file n├áo>
─Éß║ªU RA: <─æ╞░ß╗¥ng dß║½n file duy nhß║Ñt>
TI├èU CH├ì XONG: <checklist kiß╗âm ─æ╞░ß╗úc>
```

---

## 3. NG├ÇY 1 (01/11) ΓÇö PHA A: KHO KIß║╛N THß╗¿C

**Mß╗Ñc ti├¬u**: Cß║ú nh├│m tra cß╗⌐u ─æ╞░ß╗úc "ch╞░╞íng n├áo n├│i g├¼, d├╣ng v├áo ─æ├óu" m├á kh├┤ng phß║úi mß╗ƒ lß║íi PDF.
**Kho nguß╗ôn sß║╡n c├│ (cß╗Ñc bß╗Ö)**: `_Archive_CaNhan/00_Raw_PDF/_Knowledge_Base_{Larman, Nystrom, Glazer_Madhav, Fowler, Vernon}/`.
**Kho ─æß║ºu ra (─æß║⌐y GitHub)**: `Docs_BaoCao/4_KhoKienThuc/<Sach>/` ΓÇö mß╗ùi ch╞░╞íng 1 file thß║╗ kiß║┐n thß╗⌐c.

**Khu├┤n thß║╗ kiß║┐n thß╗⌐c** (mß╗ùi ch╞░╞íng): `M├ú thß║╗` ┬╖ `T├¬n ch╞░╞íng/trang` ┬╖ `├¥ ch├¡nh (Γëñ10 d├▓ng)` ┬╖ `Kh├íi niß╗çm/Pattern` ┬╖ `Khi n├áo d├╣ng` ┬╖ `Li├¬n hß╗ç vß╗¢i dß╗▒ ├ín game` ┬╖ `Thuß║¡t ngß╗»`.

| M├ú | Viß╗çc cß║ºn l├ám (1 prompt = 1 d├▓ng) | ─Éß║ºu ra | Phß╗Ñ tr├ích |
| :-: | :--- | :--- | :-: |
| **KT-01** | Chß╗æt cß║Ñu tr├║c kho, quy ╞░ß╗¢c m├ú thß║╗ (`LAR-07-02`), front-matter chuß║⌐n | `4_KhoKienThuc/README.md` | Nam |
| **KT-02** | Kiß╗âm k├¬ phß║ºn ─æ├ú b├│c t├ích sß║╡n cß╗ºa 5 s├ích: ─æß╗º ch╞░╞íng ch╞░a, lß╗ùi font/dß║Ñu kh├┤ng ΓåÆ bß║úng ─æß╗Ö phß╗º | `4_KhoKienThuc/00_DoPhu.md` | Nam |
| **KT-03aΓÇªh** | **Larman** (38 ch╞░╞íng, ~8 prompt): mß╗Ñc lß╗Ñc + thß║╗ tß╗½ng ch╞░╞íng. *╞»u ti├¬n tr╞░ß╗¢c*: UP, Use Case, Domain Model, SSD, Contract, GRASP, GoF | `4_KhoKienThuc/Larman/` | Ki├¬n |
| **KT-04aΓÇªe** | **Nystrom** (21 ch╞░╞íng, ~5 prompt). *╞»u ti├¬n*: Game Loop, State, Command, Component, Observer, Spatial Partition, Double Buffer | `4_KhoKienThuc/Nystrom/` | Anh |
| **KT-05aΓÇªc** | **Glazer & Madhav** (14 ch╞░╞íng, ~3 prompt). *╞»u ti├¬n*: kiß║┐n tr├║c client-server, snapshot, LERP, ─æß╗Ö trß╗à, ─æß╗ông bß╗Ö trß║íng th├íi | `4_KhoKienThuc/GlazerMadhav/` | Huy |
| **KT-06aΓÇªf** | **Fowler PoEAA** (~6 prompt theo nh├│m pattern: Domain Logic, Data Source, OR, Presentation, DistributionΓÇª) | `4_KhoKienThuc/Fowler/` | Ho├áng |
| **KT-07aΓÇªc** | **Vernon IDDD** (~3 prompt): Entity, Value Object, Aggregate, Repository, Domain Event | `4_KhoKienThuc/Vernon/` | Ho├áng (Nam duyß╗çt) |
| **KT-08** | Tß╗½ ─æiß╗ân thuß║¡t ngß╗» + **bß║ún ─æß╗ô kh├íi niß╗çm ch├⌐o** (GRASP Γåö GoF Γåö PoEAA Γåö DDD Γåö game patterns) | `4_KhoKienThuc/99_TuDien_va_BanDoKhaiNiem.md` | Nam |
| **KT-09** | Kiß╗âm tra ─æß╗Ö phß╗º (mß╗ìi ch╞░╞íng c├│ thß║╗, mß╗ìi thß║╗ c├│ m├ú) v├á **chß╗æt kho v1.0** | `4_KhoKienThuc/00_DoPhu.md` (cß║¡p nhß║¡t) | Cß║ú nh├│m |

**Xong Ng├áy 1 khi**: `00_DoPhu.md` b├ío 5/5 s├ích ─æß╗º thß║╗; ├¡t nhß║Ñt c├íc ch╞░╞íng *╞»u ti├¬n* ─æ├ú c├│ thß║╗ ho├án chß╗ënh.
**Nß║┐u trß╗à**: chß╗ë giß╗» c├íc ch╞░╞íng *╞»u ti├¬n*; ch╞░╞íng c├▓n lß║íi ─æß╗â dß║íng mß╗Ñc lß╗Ñc, bß╗ò sung sau ΓÇö **kh├┤ng** ─æß╗â chß║╖n Pha B.

---

## 4. NG├ÇY 2 (02/11) ΓÇö PHA B: 4 B├üO C├üO T├ôM Tß║«T

**Mß╗Ñc ti├¬u**: Mß╗ùi cuß╗æn mß╗Öt b├ío c├ío ngß║»n, ─æß╗ìc 10 ph├║t hiß╗âu ─æ╞░ß╗úc s├ích n├áy **gi├║p g├¼ cho dß╗▒ ├ín**.
**─Éß║ºu v├áo**: Chß╗ë d├╣ng thß║╗ kiß║┐n thß╗⌐c ß╗ƒ Pha A (kh├┤ng ─æß╗ìc lß║íi PDF) ΓåÆ b├ío c├ío lu├┤n khß╗¢p kho.

| M├ú | Viß╗çc cß║ºn l├ám | ─Éß║ºu ra | Phß╗Ñ tr├ích |
| :-: | :--- | :--- | :-: |
| **BC-01** | Template b├ío c├ío cß╗æ ─æß╗ïnh: *T├│m tß║»t 1 trang ┬╖ Kh├íi niß╗çm cß╗æt l├╡i ┬╖ Pattern/nguy├¬n l├╜ ┬╖ ├üp dß╗Ñng v├áo game ┬╖ Checklist khi code ┬╖ Tr├¡ch dß║½n (m├ú thß║╗)* | `4_KhoKienThuc/_Template_BaoCao.md` | Nam |
| **BC-02a/b/c** | **B├ío c├ío Larman**: (a) UP & UML ┬╖ (b) OOA/OOD & GRASP ┬╖ (c) GoF & tiß║┐n h├│a thiß║┐t kß║┐ | `Docs_BaoCao/5_BaoCaoTomTat/01_Larman.md` | Ki├¬n |
| **BC-03a/b** | **B├ío c├ío Nystrom**: (a) mß║½u thiß║┐t kß║┐ h├ánh vi/khß╗ƒi tß║ío ┬╖ (b) mß║½u tß╗æi ╞░u & v├▓ng lß║╖p | `5_BaoCaoTomTat/02_Nystrom.md` | Anh |
| **BC-04a/b** | **B├ío c├ío Glazer & Madhav**: (a) kiß║┐n tr├║c mß║íng & giao thß╗⌐c ┬╖ (b) ─æß╗ông bß╗Ö, ─æß╗Ö trß╗à, chß╗æng gian lß║¡n | `5_BaoCaoTomTat/03_GlazerMadhav.md` | Huy |
| **BC-05a/b** | **B├ío c├ío Fowler + Vernon** (gß╗Öp): (a) PoEAA ┬╖ (b) DDD, rß╗ôi ─æß╗æi chiß║┐u hai b├¬n | `5_BaoCaoTomTat/04_Fowler_Vernon.md` | Ho├áng |
| **BC-06** | **Ma trß║¡n "S├ích ΓåÆ Hß║íng mß╗Ñc ΓåÆ Package"**: mß╗ùi hß║íng mß╗Ñc trong 9 hß║íng mß╗Ñc dß╗▒a v├áo ch╞░╞íng n├áo | `5_BaoCaoTomTat/05_MaTran_Sach_HangMuc.md` | Nam |
| **BC-07** | **Review ch├⌐o**: mß╗ùi ng╞░ß╗¥i duyß╗çt 1 b├ío c├ío cß╗ºa ng╞░ß╗¥i kh├íc, kiß╗âm tra m├ú thß║╗/trang | ghi ch├║ review trong tß╗½ng file | Cß║ú nh├│m |
| **BC-08** | Chß╗æt v1.0, gß║»n v├áo Ch╞░╞íng 7 b├ío c├ío ch├¡nh thß╗⌐c | cß║¡p nhß║¡t `Chuong_07_TaiLieuThamKhao.md` | Nam |

**Xong Ng├áy 2 khi**: 4 b├ío c├ío + ma trß║¡n c├│ mß║╖t, ─æ├ú qua review ch├⌐o, kh├┤ng c├▓n `[Cß║ªN KIß╗éM TRA]` ch╞░a xß╗¡ l├╜.

---

## 5. NG├ÇY 3 S├üNG (03/11) ΓÇö PHA C: Bß╗ÿ KHUNG THß╗ÉNG NHß║ñT CHO AI

**Mß╗Ñc ti├¬u**: 5 ng╞░ß╗¥i d├╣ng AI theo **c├╣ng mß╗Öt luß║¡t** ΓåÆ thiß║┐t kß║┐/code cß╗ºa mß╗ìi ng╞░ß╗¥i gh├⌐p khß╗¢p, kh├┤ng mß╗ùi ng╞░ß╗¥i mß╗Öt kiß╗âu.
**Nß╗ün c├│ sß║╡n**: `.agent/rules/project-guide.md` (kiß║┐n tr├║c, lß╗çnh build, quy ╞░ß╗¢c hiß╗çn tß║íi) ΓåÆ **mß╗ƒ rß╗Öng, kh├┤ng viß║┐t lß║íi tß╗½ ─æß║ºu**.

| M├ú | Viß╗çc cß║ºn l├ám | ─Éß║ºu ra | Phß╗Ñ tr├ích |
| :-: | :--- | :--- | :-: |
| **AI-01** | **Hiß║┐n ch╞░╞íng dß╗▒ ├ín**: kiß║┐n tr├║c ph├ón tß║ºng, thuß║¡t ngß╗» thß╗æng nhß║Ñt, ─æiß╗üu cß║Ñm (vd: kh├┤ng `import java.awt.*` v├áo `core/combat/network/server/persistence`), ng├┤n ngß╗» trß║ú lß╗¥i | `.agent/rules/project-guide.md` (mß╗ƒ rß╗Öng) | Nam |
| **AI-02** | **Luß║¡t truy xuß║Ñt kiß║┐n thß╗⌐c**: AI phß║úi tr├¡ch m├ú thß║╗, kh├┤ng bß╗ïa trang; c├ích trß╗Å v├áo `4_KhoKienThuc/` | `.agent/rules/knowledge-rules.md` | Nam |
| **AI-03** | **Mß║½u prompt chuß║⌐n + Th╞░ viß╗çn prompt** theo loß║íi: OOA ┬╖ OOD ┬╖ code ┬╖ test ┬╖ b├ío c├ío | `Docs_BaoCao/6_BoKhungAI/01_ThuVienPrompt.md` | Nam + Ki├¬n |
| **AI-04** | **Bß╗Ö template t├ái liß╗çu thiß║┐t kß║┐**: Use Case fully dressed, SSD, Operation Contract, DCD, Sequence (Mermaid), State Machine | `6_BoKhungAI/templates/` | Ki├¬n |
| **AI-05** | **Quy ╞░ß╗¢c code & test**: ─æß║╖t t├¬n, package, c├ích th├¬m test cho mß╗ùi hß║íng mß╗Ñc (nß╗æi tiß║┐p bß╗Ö 15/15 hiß╗çn c├│) | `6_BoKhungAI/02_QuyUoc_Code_Test.md` | Ho├áng |
| **AI-06** | **Quy tr├¼nh Git + AI**: nh├ính theo ng╞░ß╗¥i, commit message, checklist tr╞░ß╗¢c khi gß╗Öp `main` (nß╗æi vß╗¢i `03_HuongDan_Git_Nhom.md`) | `6_BoKhungAI/03_QuyTrinh_Git_AI.md` | Nam |
| **AI-07** | **Chß║íy thß╗¡ (pilot)** bß╗Ö khung tr├¬n **1 chß╗⌐c n─âng nhß╗Å** (─æß╗ü xuß║Ñt: *Sß║únh chß╗¥*) tß╗½ Use Case ─æß║┐n DCD, ghi lß║íi chß╗ù khung bß╗ï thiß║┐u/m├óu thuß║½n | `6_BoKhungAI/04_KetQua_Pilot.md` | Huy |
| **AI-08** | Sß╗¡a bß╗Ö khung theo kß║┐t quß║ú pilot v├á **chß╗æt v1.0** | cß║¡p nhß║¡t c├íc file AI-01ΓÇª06 | Nam |

**Xong buß╗òi s├íng khi**: pilot chß║íy tr╞ín tß╗½ UC ΓåÆ DCD m├á kh├┤ng phß║úi "giß║úi th├¡ch th├¬m bß║▒ng miß╗çng".

---

## 6. NG├ÇY 3 CHIß╗ÇU ΓåÆ NG├ÇY 4 (03/11 ΓÇô 04/11) ΓÇö PHA D: THIß║╛T Kß║╛ OOA ΓåÆ OOD ΓåÆ DESIGN

**Mß╗Ñc ti├¬u**: Thiß║┐t kß║┐ **gß║ºn nh╞░ to├án bß╗Ö chß╗⌐c n─âng** cß╗ºa dß╗▒ ├ín (phß║ºn ─æ├ú c├│ + 9 hß║íng mß╗Ñc mß╗ƒ rß╗Öng) ─æß╗â tß╗½ 05/11 code l├á "dß╗ïch thiß║┐t kß║┐ ra Java".
**Th╞░ mß╗Ñc ─æß║ºu ra**: `Docs_BaoCao/7_ThietKe/{OOA, OOD, Design}/`. Mß╗ìi t├ái liß╗çu d├╣ng template ß╗ƒ AI-04 v├á tr├¡ch thß║╗ kiß║┐n thß╗⌐c.

### 6.1. D1 ΓÇö OOA (03/11 chiß╗üu)

| M├ú | Viß╗çc cß║ºn l├ám | Phß╗Ñ tr├ích |
| :-: | :--- | :-: |
| **OOA-01** | **Kiß╗âm k├¬ chß╗⌐c n─âng**: c├íi ─æ├ú c├│ + 9 hß║íng mß╗Ñc; g├ín ─æß╗Ö ╞░u ti├¬n **P1/P2/P3** v├á quan hß╗ç phß╗Ñ thuß╗Öc | Nam + Ki├¬n |
| **OOA-02** | S╞í ─æß╗ô Use Case tß╗òng + **Use Case brief** cho to├án bß╗Ö chß╗⌐c n─âng | Ki├¬n |
| **OOA-03** | **Fully dressed** cho UC ╞░u ti├¬n, mß╗ùi ng╞░ß╗¥i mß╗Öt cß╗Ñm: Sß║únh chß╗¥ & V├▓ng bo (Huy) ┬╖ Map & Mobs (Ho├áng) ┬╖ Hß╗ç nh├ón vß║¡t & Bot (Ki├¬n) ┬╖ ─Éß╗ô hß╗ìa 2.5D (Anh) ┬╖ Core/─Éiß╗üu khiß╗ân (Nam) | Tß╗½ng ng╞░ß╗¥i |
| **OOA-04** | **Domain Model v2** (kh├íi niß╗çm thß║┐ giß╗¢i thß║¡t, ch╞░a c├│ method) | Nam |
| **OOA-05** | **SSD + Operation Contract** cho c├íc thao t├íc hß╗ç thß╗æng ch├¡nh | Huy (lobby/mß║íng) ┬╖ Ki├¬n (chiß║┐n ─æß║Ñu) |
| **OOA-06** | **Supplementary Spec (FURPS+)**: 60 FPS, server 20 Hz, ─æß╗Ö trß╗à, sß╗æ ng╞░ß╗¥i, bß║ún ─æß╗ô 66.000 px | Ho├áng |

### 6.2. D2 ΓÇö OOD (04/11 s├íng)

| M├ú | Viß╗çc cß║ºn l├ám | Phß╗Ñ tr├ích |
| :-: | :--- | :-: |
| **OOD-01** | **Kiß║┐n tr├║c logic & bß║ún ─æß╗ô package v2** (lß╗¢p n├áo thuß╗Öc tß║ºng n├áo) | Nam |
| **OOD-02** | **S╞í ─æß╗ô t╞░╞íng t├íc (Sequence)** cho tß╗½ng UC, chß╗ë r├╡ GRASP (Controller, Expert, CreatorΓÇª) | Tß╗½ng ng╞░ß╗¥i |
| **OOD-03** | **DCD v2** tß╗½ng cß╗Ñm, sau ─æ├│ Nam hß╗úp nhß║Ñt ─æß╗â kh├┤ng tr├╣ng/lß╗çch t├¬n lß╗¢p | Tß╗½ng ng╞░ß╗¥i ΓåÆ Nam |
| **OOD-04** | **State Machine**: FSM nh├ón vß║¡t, luß╗ông Sß║únh chß╗¥ ΓåÆ Trß║¡n ΓåÆ Kß║┐t th├║c, trß║íng th├íi V├▓ng bo | Nam ┬╖ Huy |
| **OOD-05** | **Thiß║┐t kß║┐ giao thß╗⌐c mß║íng**: g├│i tin, snapshot, ─æß╗ông bß╗Ö Mobs/Bot, relay 4G | Huy |
| **OOD-06** | **Thiß║┐t kß║┐ dß╗» liß╗çu & cß║Ñu h├¼nh** (6 Mobs, 4 hß╗ç nh├ón vß║¡t, v├▓ng bo) theo Fowler/DDD | Ho├áng |
| **OOD-07** | **Bß║úng ├ính xß║í GoF/GRASP ΓåÆ lß╗¢p** (mß║½u ─æ├ú chß╗ìn + l├╜ do + s├ích n├áo) | Ki├¬n |
| **OOD-08** | **Thiß║┐t kß║┐ pipeline render 2.5D**: Z-Sorting, culling, sprite/mobs | Anh |

### 6.3. D3 ΓÇö DESIGN CHI TIß║╛T & CHß╗ÉT (04/11 chiß╗üu)

| M├ú | Viß╗çc cß║ºn l├ám | Phß╗Ñ tr├ích |
| :-: | :--- | :-: |
| **DES-01** | **Luß╗ông m├án h├¼nh & wireframe**: Sß║únh chß╗¥, chß╗ìn T╞░ß╗¢ng/Team/Bot, HUD, minimap, v├▓ng bo | Anh + Huy |
| **DES-02** | **Bß║úng sß╗æ liß╗çu thiß║┐t kß║┐**: chß╗ë sß╗æ 4 hß╗ç nh├ón vß║¡t, 6 Mobs, th├┤ng sß╗æ v├▓ng bo | Ki├¬n + Ho├áng |
| **DES-03** | **Kß║┐ hoß║ích test & ti├¬u ch├¡ nghiß╗çm thu** cho tß╗½ng hß║íng mß╗Ñc | Ho├áng |
| **DES-04** | **Ma trß║¡n truy vß║┐t**: Y├¬u cß║ºu ΓåÆ UC ΓåÆ Lß╗¢p ΓåÆ Test | Nam |
| **DES-05** | **Review to├án bß╗Ö, ─æ├│ng b─âng thiß║┐t kß║┐ v1.0**, chia task code cho tß╗½ng ng╞░ß╗¥i tß╗½ 05/11 | Cß║ú nh├│m |

**Xong Ng├áy 4 khi**: mß╗ùi hß║íng mß╗Ñc c├│ ─æß╗º chuß╗ùi `UC ΓåÆ Sequence ΓåÆ DCD ΓåÆ Test nghiß╗çm thu`; DES-05 ─æ├ú k├╜ duyß╗çt.

---

## 7. Rß╗ªI RO & PH╞»╞áNG ├üN Dß╗░ PH├ÆNG

| Rß╗ºi ro | Dß║Ñu hiß╗çu | C├ích xß╗¡ l├╜ |
| :--- | :--- | :--- |
| Pha A k├⌐o d├ái v├¼ s├ích qu├í d├ái | 18h ng├áy 01/11 vß║½n ch╞░a xong | Chß╗ë giß╗» c├íc ch╞░╞íng *╞»u ti├¬n*, phß║ºn c├▓n lß║íi ─æß╗â mß╗Ñc lß╗Ñc, **kh├┤ng chß║╖n** Pha B |
| AI t├│m tß║»t sai/bß╗ïa trang | B├ío c├ío c├│ trang kh├┤ng tß╗ôn tß║íi | Luß║¡t AI-02 + review ch├⌐o BC-07; ghi `[Cß║ªN KIß╗éM TRA]` |
| Mß╗ùi ng╞░ß╗¥i thiß║┐t kß║┐ mß╗Öt kiß╗âu | DCD t├¬n lß╗¢p lß╗çch nhau | D├╣ng template AI-04 + Nam hß╗úp nhß║Ñt ß╗ƒ OOD-03 |
| Pha D qu├í nhiß╗üu so vß╗¢i 1,5 ng├áy | Hß║┐t 04/11 ch╞░a kß╗ïp Design | **L├ám theo thß╗⌐ tß╗▒ ╞░u ti├¬n P1 ΓåÆ P2 ΓåÆ P3**; hß║íng mß╗Ñc P3 ─æß╗â dß║íng Use Case brief, bß╗ò sung sau, **kh├┤ng** tß╗Ñt tiß║┐n ─æß╗Ö code P1 |
| Pilot AI-07 lß╗Ö lß╗ùi lß╗¢n cß╗ºa bß╗Ö khung | Pilot phß║úi giß║úi th├¡ch th├¬m bß║▒ng miß╗çng | D├ánh ─æß╗º AI-08 ─æß╗â sß╗¡a; ch╞░a chß╗æt khung th├¼ ch╞░a v├áo Pha D |

---

## 8. THEO D├òI TIß║╛N ─Éß╗ÿ

─É├ính dß║Ñu khi **─æ├ú duyß╗çt + commit** (kh├┤ng phß║úi khi AI vß╗½a sinh xong):

* **Pha A**: ΓÿÉ KT-01 ΓÿÉ KT-02 ΓÿÉ KT-03 ΓÿÉ KT-04 ΓÿÉ KT-05 ΓÿÉ KT-06 ΓÿÉ KT-07 ΓÿÉ KT-08 ΓÿÉ KT-09
* **Pha B**: ΓÿÉ BC-01 ΓÿÉ BC-02 ΓÿÉ BC-03 ΓÿÉ BC-04 ΓÿÉ BC-05 ΓÿÉ BC-06 ΓÿÉ BC-07 ΓÿÉ BC-08
* **Pha C**: ΓÿÉ AI-01 ΓÿÉ AI-02 ΓÿÉ AI-03 ΓÿÉ AI-04 ΓÿÉ AI-05 ΓÿÉ AI-06 ΓÿÉ AI-07 ΓÿÉ AI-08
* **Pha D1 (OOA)**: ΓÿÉ OOA-01 ΓÿÉ OOA-02 ΓÿÉ OOA-03 ΓÿÉ OOA-04 ΓÿÉ OOA-05 ΓÿÉ OOA-06
* **Pha D2 (OOD)**: ΓÿÉ OOD-01 ΓÿÉ OOD-02 ΓÿÉ OOD-03 ΓÿÉ OOD-04 ΓÿÉ OOD-05 ΓÿÉ OOD-06 ΓÿÉ OOD-07 ΓÿÉ OOD-08
* **Pha D3 (Design)**: ΓÿÉ DES-01 ΓÿÉ DES-02 ΓÿÉ DES-03 ΓÿÉ DES-04 ΓÿÉ DES-05

> Ph├ón c├┤ng tr├¬n l├á **─æß╗ü xuß║Ñt** theo s├ích/ph├ón hß╗ç mß╗ùi ng╞░ß╗¥i ─æang phß╗Ñ tr├ích (Ki├¬nΓÇôLarman & Combat, AnhΓÇôNystrom & Render, HuyΓÇôGlazer & Mß║íng, Ho├ángΓÇôFowler/Vernon & Dß╗» liß╗çu/Test, NamΓÇôKiß║┐n tr├║c & tß╗òng hß╗úp). Nh├│m c├│ thß╗â ─æß╗òi ng╞░ß╗¥i cho c├ón tß║úi.
