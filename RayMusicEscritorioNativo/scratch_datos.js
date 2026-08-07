const DATOS_CATEGORIAS = [
  {
    "name": "Radio",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features221/v4/e4/6e/84/e46e84fa-1fbf-b795-7cee-9a0f7009040e/99630181-a2c8-46ca-adec-f4a1186a4150.png/290x163sr.webp"
  },
  {
    "name": "Conciertos",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features221/v4/20/7a/cd/207acdb0-beaf-2a7b-81c0-643ae3c73bb7/a09e1918-cb88-4b25-9179-f38d15502d22.png/290x163sr.webp"
  },
  {
    "name": "Éxitos",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/3e/2e/ed/3e2eeda6-984a-6324-e0cf-7bb576cdd91b/5c1f16e0-8cbb-45ad-9f93-ea48a8ac0cb5.png/290x163sr.webp"
  },
  {
    "name": "Charts",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features221/v4/62/61/c4/6261c465-fe72-eb32-5687-b7c015064b39/77b7bb43-5e28-4c40-afbc-97904ab8636a.png/290x163sr.webp"
  },
  {
    "name": "Hip-hop-rap",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/29/91/b1/2991b17d-fd89-333b-ec0d-46a3a5a2d5ad/da38fd19-e817-4160-9f7d-9f658a14c26e.png/290x163sr.webp"
  },
  {
    "name": "Latinoamérica",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/21/3a/a3/213aa346-1bc9-f638-72e4-21c8934c29bf/63a97f49-a153-4727-af5f-bf89c8645806.png/290x163sr.webp"
  },
  {
    "name": "Pop latino",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/c7/6e/7d/c76e7d19-747d-37a1-c3d1-2f73d66b0b15/4846fcb9-336a-4446-9a6c-debaae640138.png/290x163sr.webp"
  },
  {
    "name": "Urbano latino",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features211/v4/2d/ce/81/2dce81e0-6d73-201a-92c1-ed4396aa763d/d9f992ca-60de-4c0b-a693-567fa306cf17.png/290x163sr.webp"
  },
  {
    "name": "Rock y alternativa",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features221/v4/b7/c2/ea/b7c2ea5b-4177-8d09-3739-e8a1edabbbda/d784c33e-831c-4bd1-9855-676d3609e828.png/290x163sr.webp"
  },
  {
    "name": "Pop español",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/25/56/77/2556772a-667e-4e12-712e-d87210c60310/5b5afe9c-fd0a-4d9a-98f5-db9cee277f12.png/290x163sr.webp"
  },
  {
    "name": "Rock español",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features211/v4/73/27/4e/73274e21-5f2b-be22-88e7-0b54943dbf03/da87eaf5-916f-49bc-a10a-de5ab11374ff.png/290x163sr.webp"
  },
  {
    "name": "Dance",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/c1/c0/66/c1c06690-0bcd-d60d-1e3d-a702bad679e9/3c7ab098-157c-45d0-994b-b8d5594f89c5.png/290x163sr.webp"
  },
  {
    "name": "Rock",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/be/0b/fc/be0bfc31-14e9-4da3-e895-685523e22d14/ef1fe7da-93cb-4c5f-ba6a-2fd00f238d4e.png/290x163sr.webp"
  },
  {
    "name": "Reggae",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features211/v4/0d/ae/a0/0daea000-e59a-5793-2c38-899c7541f041/b137a695-dd77-4218-8d16-9e933e8fd156.png/290x163sr.webp"
  },
  {
    "name": "Fiesta",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features211/v4/13/12/7d/13127dc4-9c81-099c-31a7-afb849518840/06e568d2-c588-448f-8721-1739e6ac2f2f.png/290x163sr.webp"
  },
  {
    "name": "Pop",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/78/6f/74/786f7419-b80c-1fe6-b9f3-b2b7a7532822/f23bd272-c6ad-441c-8a80-82b8c0956954.png/290x163sr.webp"
  },
  {
    "name": "Chill",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features221/v4/0f/17/d5/0f17d5a3-6774-1ae1-4530-2b694d8fb6bf/d7944211-2928-4ccc-b382-f0564bcf00b2.png/290x163sr.webp"
  },
  {
    "name": "Amor",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/64/54/4d/64544d03-52cf-3200-e554-d742dcdaa58b/1792da44-b8f1-4eb7-9f07-af7e01ed7b32.png/290x163sr.webp"
  },
  {
    "name": "Electrónica",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features211/v4/e2/51/f1/e251f15a-36f4-93de-e0e8-ee90de2a4ebc/d763cb91-7a93-4579-9a19-88c122889349.png/290x163sr.webp"
  },
  {
    "name": "Tropical",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features126/v4/8b/4a/5e/8b4a5e8e-8719-51c1-d365-2ca5ba51ee16/0c858686-461b-418b-938e-75dd88c71d5b.png/290x163sr.webp"
  },
  {
    "name": "Fitness",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features221/v4/cf/2f/ef/cf2fef65-9f86-d368-eedc-d18ad4511069/301c121f-9f4a-4de4-8f2d-f3253ee7fb3a.png/290x163sr.webp"
  },
  {
    "name": "Música mexicana",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/78/1e/f4/781ef4ea-fdc4-037a-e27b-d33ed99018dd/19efa531-fc16-4a2c-9d46-429a9a5d319e.png/290x163sr.webp"
  },
  {
    "name": "Infantil",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features211/v4/d3/94/c0/d394c010-2581-bd86-0a1c-fc0db57dd254/3eae3457-602c-438c-956e-b9f667ddd577.png/290x163sr.webp"
  },
  {
    "name": "Para dormir",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features211/v4/66/44/63/6644636a-6134-e464-32e6-a7900d583ce8/bcb74429-1303-4fb0-9c3f-a6f0b87eb86e.png/290x163sr.webp"
  },
  {
    "name": "Videos musicales",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features211/v4/3f/11/61/3f11615b-7131-9d7f-73b8-1dd2be3df94e/a9f44d74-8142-48ab-a39e-018790db37e4.png/290x163sr.webp"
  },
  {
    "name": "Country",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/47/f4/09/47f409b8-e3f9-b6bc-db46-d3e1efdf838a/165e5942-1ff1-4bde-aa4a-997009434c8d.png/290x163sr.webp"
  },
  {
    "name": "Blues",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/9f/cb/28/9fcb28ce-d34a-c841-78ec-79dd2067e6e7/1ba54c60-a74e-4511-a925-f7cffb00f6a9.png/290x163sr.webp"
  },
  {
    "name": "Clásica",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/af/d4/3d/afd43d50-d6b8-6666-f0a1-8f9172766bf7/155aae32-1dd9-47c7-93d6-e1596081d3e0.png/290x163sr.webp"
  },
  {
    "name": "Afrobeats",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/c0/3c/07/c03c0700-0347-b8f4-a2ee-0dfba31c7306/0f95bb6c-203f-4e75-8adb-161b95df24c0.png/290x163sr.webp"
  },
  {
    "name": "Amapiano",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features221/v4/a8/6d/f3/a86df34e-0c48-9d0c-bfa9-c57afd0f7f7e/3f2be728-c9a0-44df-9b6d-b53d61af38af.png/290x163sr.webp"
  },
  {
    "name": "Buena vibra",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features211/v4/30/e6/42/30e64278-008f-b3df-6790-bdd7fe382360/f3639799-0252-4a92-b3d4-3df02f586e29.png/290x163sr.webp"
  },
  {
    "name": "Jazz",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features221/v4/47/80/9f/47809ffa-4c37-09ec-b2f0-172eebcc0eb6/7a73c91e-53f2-4043-8ec6-7255f51e302e.png/290x163sr.webp"
  },
  {
    "name": "Up Next",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features211/v4/3b/eb/1c/3beb1cc8-25c4-7545-3442-b943a0a3ce26/ad009554-cc79-40fa-b03c-909f29089987.png/290x163sr.webp"
  },
  {
    "name": "Músicas del mundo",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features116/v4/d7/a0/11/d7a011c4-eac7-8660-17a4-8c995f160d42/03ae5886-1c46-446b-b18f-95b9fe189d2d.png/290x163sr.webp"
  },
  {
    "name": "DJ Mixes",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features211/v4/cb/54/58/cb5458e8-81ad-2e95-4d74-bb26a2ad9767/6b3f075b-121e-4952-837a-ede4716a51f1.png/290x163sr.webp"
  },
  {
    "name": "Essentials",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features221/v4/34/52/ae/3452ae8d-6eb5-c04f-529e-ba5336237a28/0ac87058-2f80-4bcc-926d-425a4398cc05.png/290x163sr.webp"
  },
  {
    "name": "Metal",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features221/v4/af/38/43/af38433c-534c-a343-80bb-16fe57286680/4f366afd-c381-4d9c-a270-1eb1342fa7c2.png/290x163sr.webp"
  },
  {
    "name": "Rock clásico",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/ee/0f/04/ee0f04df-41fa-5a91-346d-677baf0703d0/aa4c4050-2b2a-475e-ab94-89f70c4c5a11.png/290x163sr.webp"
  },
  {
    "name": "Concentración",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features211/v4/32/52/4d/32524d36-af28-8bfe-ac7c-66494ee10362/8540fcf4-af4a-4943-af34-1355797f90e1.png/290x163sr.webp"
  },
  {
    "name": "Bienestar",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features221/v4/e2/e4/ef/e2e4ef7e-2611-91a2-bd17-faefa1ac23a7/009cfa78-893d-4e6a-8c8d-2592de8b83f7.png/290x163sr.webp"
  },
  {
    "name": "Motivación",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features221/v4/01/42/38/0142387d-608e-45d2-c839-fb534d5d4259/1e691432-e96a-4042-aea5-79c6d0bbb9af.png/290x163sr.webp"
  },
  {
    "name": "R&B",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/8f/49/08/8f490822-4ce3-6846-d1b4-1a6adf39567e/f2292ce4-f047-4b08-8456-256fb77bfe06.png/290x163sr.webp"
  },
  {
    "name": "Indie",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features221/v4/b6/c8/81/b6c88139-a688-9be3-871e-27a863b2e46b/45257919-7df2-41d1-8fc8-b4fd9a28817e.png/290x163sr.webp"
  },
  {
    "name": "Décadas",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features211/v4/56/76/7d/56767d51-89b9-e6ce-2e7b-c8c3cf954f47/e23544e8-6aad-45e7-b555-5695fd5f883a.png/290x163sr.webp"
  },
  {
    "name": "K-pop",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/59/df/2a/59df2a68-998e-ba92-5eeb-1125b02c31c1/dd5dd123-9739-44e9-b840-a0a572f82352.png/290x163sr.webp"
  },
  {
    "name": "Cine, TV y teatro",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features221/v4/4d/e5/4f/4de54fa0-6fef-6f45-9248-3ea6e2675382/2ee7bdc8-4a5d-422a-85bf-b3c0ecf53ef3.png/290x163sr.webp"
  },
  {
    "name": "Música cristiana",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features211/v4/13/89/a7/1389a7c8-b58b-4406-38db-cfe53430c025/27b435bd-0838-4226-ba7f-0421bdf05d7d.png/290x163sr.webp"
  },
  {
    "name": "Detrás de la música",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features221/v4/ad/69/1c/ad691cf0-f626-6050-3361-28e0995849bf/f964cbb9-d787-4faf-97b8-73958872c4c1.png/290x163sr.webp"
  },
  {
    "name": "J-hip-hop",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features221/v4/d3/2b/fe/d32bfe0b-6894-3027-478e-23e6924ba8e7/4a5b9fe8-6ead-4bf0-802c-83d308e0a92a.png/290x163sr.webp"
  },
  {
    "name": "Orgullo LGBTTTIQ+",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/93/50/96/935096dc-0171-d035-10a0-55edad743b6c/cc569d6f-a84a-492a-8806-4752098c327c.png/290x163sr.webp"
  },
  {
    "name": "Sonidos del verano",
    "url": "https://is1-ssl.mzstatic.com/image/thumb/Features221/v4/53/df/78/53df782f-d953-e464-6da1-00383ebe56e4/17911dab-74b6-45c0-a1ec-bbfae0ab2ec8.png/290x163sr.webp"
  }
];