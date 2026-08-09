import * as cheerio from 'cheerio';

export class Anichin {
  constructor(domain = "https://anichin.cafe") {
    this.baseURL = domain;
    this.headers = {
      'user-agent': 'Mozilla/5.0 (Linux; Android 16; NX729J) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.7499.34 Mobile Safari/537.36',
    };
  }

  // Helper internal untuk menggantikan perilaku Axios
  async request(path, options = {}) {
    const url = path.startsWith('http') ? path : `${this.baseURL}${path}`;
    const response = await fetch(url, {
      method: options.method || 'GET',
      headers: { ...this.headers, ...options.headers },
      ...options
    });

    if (!response.ok) {
      throw new Error(`HTTP error! Status: ${response.status}`);
    }

    // Mengembalikan data text (HTML) seperti perilaku Axios `.data`
    return await response.text();
  }

  async home() {
    try {
      const data = await this.request('/');
      const $ = cheerio.load(data);
      return {
        latest: this.articleParser($, '.bixbox:eq(1) article .bsx'),
        popular: this.articleParser($, '.bixbox:eq(0) article .bsx'),
        ongoing: $(".ongoingseries li a").map((_, li) => {
          return {
            name: $(li).find(".l").text(),
            episode: parseInt($(li).find(".r").text().split(' ')[1]),
            url: $(li).attr("href"),
          };
        }).get()
      };
    } catch (error) {
      throw new Error(error.message);
    }
  }

  async latest() {
    try {
      const data = await this.request('/');
      return {
        list: this.articleParser(cheerio.load(data), '.bixbox:eq(1) article .bsx')
      };
    } catch (error) {
      throw new Error(error.message);
    }
  }

  async popular() {
    try {
      const data = await this.request('/');
      return {
        list: this.articleParser(cheerio.load(data), '.bixbox:eq(0) article .bsx')
      };
    } catch (error) {
      throw new Error(error.message);
    }
  }

  async complete(page = 1) {
    try {
      const data = await this.request('/complete/page/' + page);
      const $ = cheerio.load(data);
      const pg = $(".pagination .next").attr("href");
      return {
        list: this.articleParser($, '.listupd article .bsx'),
        hasNextPage: !!pg,
        nextPage: pg?.match(/page\/(.*?)\//)?.[1] || null,
      };
    } catch (error) {
      throw new Error(error.message);
    }
  }

  async schedule() {
    try {
      const data = await this.request('/schedule');
      const $ = cheerio.load(data);
      return {
        list: $('.postbody .schedulepage').map((_, el) => {
          return {
            day: $(el).find('.releases').text().toLowerCase(),
            list: this.articleParser2($)
          };
        }).get()
      };
    } catch (error) {
      throw new Error(error.message);
    }
  }

  async search(query, page = 1) {
    try {
      // Fetch query string parameters menggunakan URLSearchParams bawaan Node.js/Browser
      const params = new URLSearchParams({ s: query }).toString();
      const data = await this.request(`/page/${page}?${params}`);
      const $ = cheerio.load(data);
      const pg = $(".pagination .next").attr("href");
      return {
        list: this.articleParser($, '.bixbox article .bsx'), // Diperbaiki dari 'data' menjadi '$' agar cheerio bekerja
        hasNextPage: !!pg,
        nextPage: pg?.match(/page\/(.*?)\//)?.[1] || null,
      };
    } catch (error) {
      throw new Error(error.message);
    }
  }

  async episode(url) {
    try {
      if (this.validateUrlPage(url) !== 'episode') throw new Error('Invalid url.');
      
      const data = await this.request(url);
      const $ = cheerio.load(data);
      const ep = url.match(/episode-(.*?)-subtitle/)?.[1]?.split('-');
      return {
        title: $('div.headlist .det').find('a').text(),
        ...(ep?.[1] ? {
          episode: parseInt(ep[0]),
          end: true
        } : {
          episode: parseInt(ep[0])
        }),
        embed: await Promise.all($('select.mirror option').map(async (_, e) => {
          if (!$(e).attr('data-index')) return null;
          let stream, urlx = Buffer.from($(e).attr('value'), 'base64').toString().match(/src="(.*?)"/)?.[1];
          if (urlx.includes('anichin.stream')) stream = await this.parseStream(urlx);
          return {
            name: $(e).text().trim(),
            url: urlx,
            ...(stream ? { stream } : {})
          };
        }).get()).then(f => f.filter(Boolean)),
        download: $('.soraurlx').map((_, e) => {
          return {
            reso: $(e).find('strong').text(),
            list: $(e).find('a').map((_, x) => ({
              service: $(x).text(),
              url: $(x).attr('href')
            })).get()
          };
        }).get()
      };
    } catch (error) {
      throw new Error(error.message);
    }
  }

  async detail(url) {
    try {
      if (this.validateUrlPage(url) !== 'detail') throw new Error('Invalid url.');
      const data = await this.request(url);
      const [$, r] = [cheerio.load(data), {}];
      
      $(".spe span").each((_, el) => {
        const fullLabel = $(el).find("b").first().text();
        const key = fullLabel.replace(/\s+/g, " ").trim().replace(/:\s*$/, "").toLowerCase().replace(/[^\w]+/g, "_").replace(/^_+|_+$/g, "");
        const $t = $(el).find("time").first();
        const $a = $(el).find("a");
        
        if (!key) return;
        if ($t.length) {
          r[key] = {
            text: $t.text().trim(),
            datetime: $t.attr("datetime") || null,
          };
          return;
        }
        if ($a.length) {
          r[key] = $a.map((_, e) => $(e).text()).get();
          return;
        }
        if ($(el).find("i.fn").length) return;
        
        const fullText = $(el).text().replace(/\s+/g, " ").trim();
        const value = fullText.replace(fullLabel, "").trim(); // Memperbaiki 'label' yang tidak terdefinisi pada kode awal Anda
        r[key] = value || null;
      });
      
      return {
        title: $('.entry-title').text(),
        alternativeTitles: $('.alter').text(),
        cover: $('.thumb img').attr('src'),
        synopsis: $('.synp .entry-content').text().trim(),
        ...r,
        genres: $('.genxed a').map((_, e) => $(e).text()).get(),
        episode: $('.eplister ul li a').map((_, e) => {
          const ep = $(e).find('.epl-num').text();
          return {
            ...(ep.includes('END') ? {
              episode: parseInt(ep.split(' ')[0]),
              end: true
            } : {
              episode: parseInt(ep)
            }),
            date: $(e).find('.epl-date').text(),
            url: $(e).attr('href')
          };
        }).get()
      };
    } catch (error) {
      throw new Error(error.message);
    }
  }

  validateUrlPage(url) {
    if (url.includes('/seri')) {
      return 'detail';
    } else if (url.match(/episode-\d/)) {
      return 'episode';
    }
  }

  async parseStream(url) {
    try {
      const fixUrl = url.replace('?id=', 'hls/') + '.m3u8';
      const re = await this.request(fixUrl);
      const lines = re.split('\n').map(l => l.trim()).filter(Boolean);
      let variants = [], current = null;
    
      for (const line of lines) {
        if (line.startsWith('#EXT-X-STREAM-INF:')) {
          current = line.slice(19);
          continue;
        }
        if (current && !line.startsWith('#')) {
          const attrs = Object.fromEntries(
            current.split(',').map(p => {
              const [k, v = ''] = p.split('=');
              return [k, v.replace(/^"|"$/g, '')];
            })
          );
          variants.push({
            bandwidth: Number(attrs.BANDWIDTH) || 0,
            resolution: attrs.RESOLUTION || null,
            url: line
          });
          current = null;
        }
      }
      return variants;
    } catch(e) {
      throw e;
    }
  }

  articleParser($, a) {
    return $(a).map((_, el) => ({
      title: $(el).find('.tt h2').text().trim(),
      type: $(el).find('.typez').text().trim(),
      episode: parseInt($(el).find('.bt .epx').text().split(' ')[1]) || $(el).find('.bt .epx').text(),
      url: $(el).find('a').attr('href'),
      cover: $(el).find('img').attr('src')
    })).get();
  }

  articleParser2($, a) {
    return $('.listupd .bs .bsx').map((_, el) => ({
      title: $(el).find('.tt').text().trim(),
      time: $(el).find('.cndwn').text().split(' ')[1],
      cover: $(el).find('img').attr('src'),
      url: $(el).find('a').attr('href'),
    })).get();
  }
}

const an = new Anichin
const lat = await an.episode('https://anichin.cafe/lingwu-continent-episode-179-subtitle-indonesia/')
console.log(lat)
/*
hasilnya ini umumnya dailymotion sama ok.ru
{
  title: 'Lingwu Continent',
  episode: 179,
  embed: [
    {
      name: 'Premium 1',
      url: 'https://anichin.stream/?id=v77tviw',
      stream: [Array]
    },
    { name: 'OK.ru', url: 'https://ok.ru/videoembed/14309808343730' },
    {
      name: 'Dailymotion [Ads]',
      url: 'https://geo.dailymotion.com/player/xid0t.html?video=k5lJUqnyR4XmXXG47Vc'
    },
    {
      name: 'Rumble [Ads]',
      url: 'https://rumble.com/embed/v77tviw/?pub=2li51c'
    },
    {
      name: 'Drive 1 [Ads]',
      url: 'https://abyssplayer.com/L1Dk5KE8OC'
    },
    {
      name: 'Drive 2 [Ads]',
      url: 'https://rubyvidhub.com/embed-gutl2qg5wx88.html'
    }
  ],
  download: [
    { reso: '360p', list: [Array] },
    { reso: '480p', list: [Array] },
    { reso: '720p', list: [Array] },
    { reso: '1080p', list: [Array] }
  ]
}
*/