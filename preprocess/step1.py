import logging
import os
import re
import sqlite3
import subprocess

import sys


class Entri:
    def __init__(self):
        self.eid = None
        self.nilai = None
        self.maknas = []
        self.jenis_rujuk = None
        self.entri_rujuk = None
        self.acu_rujuks = []  # processed data by looking up entri and acu
        self.induk = None
        self.anaks = []
        self.silabel = None
        self.acu = None
        self.jenis = None
        self.entri_var = None
        self.lafal = None

    def __repr__(self):
        return u"Entri<{} '{}' {}>".format(self.eid, self.nilai, self.maknas)

    def __lt__(self, other):
        return self.eid < other.eid


class Makna:
    def __init__(self):
        self.mid = None
        self.nilai = None
        self.kelas = None
        self.bahasa = None
        self.bidang = None
        self.ilmiah = None
        self.kimia = None
        self.is_ki = None
        self.is_kp = None
        self.is_akr = None
        self.ragams = []
        self.contohs = []

    def __repr__(self):
        return "Makna<{} '{}'>".format(self.mid, self.nilai)


class Contoh:
    def __init__(self):
        self.cid = None
        self.nilai = None

    def __repr__(self):
        return "Contoh<{} '{}'>".format(self.cid, self.nilai)


class Acu:
    def __init__(self):
        self.aid = None
        self.nilai = None
        self.entries = []

    def __repr__(self):
        return u"Acu<{} '{}' {}>".format(self.aid, self.nilai, self.entries)

    def __lt__(self, other):
        return self.aid < other.aid


class Kategori:
    def __init__(self):
        self.jenis = None
        self.nilai = None
        self.desc = None
        self.urutan = None


index_acu_nilai = {}
index_entri_eid = {}
index_entri_nilai = {}
all_acus = []
all_entries = []
all_kategoris = []


def canonize(s: str):
    s = s.lower()
    # TODO diacritics
    m = re.match(r'(.*?)\s*\(\d+\)', s)
    if m:
        return m.group(1)
    return s


conn = sqlite3.connect('in/kbbi5v5.db')

# Bikin index supaya query di bawah pasti cepet
conn.execute('create index if not exists Contoh_aktif on Contoh(aktif)')
conn.execute('create index if not exists Entri_aktif on Entri(aktif)')
conn.execute('create index if not exists Makna_eid_aktif on Makna(eid, aktif)')
conn.execute('create index if not exists Contoh_mid_aktif on Contoh(mid, aktif)')
conn.execute('create index if not exists Kategori_aktif on Kategori(aktif)')

mids_with_contoh = set()
for row in conn.execute('select distinct mid from Contoh where aktif=1').fetchall():
    mids_with_contoh.add(row[0])

print('mids_with_contoh count:', len(mids_with_contoh))

# Singkatan dalam makna atau contoh
INLINE_SINGKATAN = {
    'akr': 'akronim',
    'dl': 'dalam',
    'dll': 'dan lain-lain',
    'dng': 'dengan',
    'dp': 'daripada',
    'dr': 'dari',
    'dsb': 'dan sebagainya',
    'dst': 'dan seterusnya',
    'ki': 'kias',
    'kp': 'kependekan',
    'kpd': 'kepada',
    'krn': 'karena',
    'msl': 'misalnya',
    'pb': 'peribahasa',
    'pd': 'pada',
    'sbg': 'sebagai',
    'spt': 'seperti',
    'thd': 'terhadap',
    'tsb': 'tersebut',
    'tt': 'tentang',
    'yg': 'yang',
}

# compile regex to optimize
_inline_singkatan = [
    (k, re.compile(r'\b{}\b'.format(k)), v)
    for k, v in INLINE_SINGKATAN.items()
]

def expand_inline_singkatan(s):
    for _is in _inline_singkatan:
        if _is[0] in s:
            s = re.sub(_is[1], _is[2], s)
    return s

for row in conn.execute('select eid, entri, jenis_rujuk, entri_rujuk, induk, silabel, jenis, entri_var, lafal from Entri where aktif=1').fetchall():
    e = Entri()
    e.eid = row[0]
    e.nilai = row[1]

    # get makna
    for rowm in conn.execute('select mid, makna, kelas, bahasa, bidang, ilmiah, kimia, ki, kp, akr, ragam, ragam_var from Makna where eid=? and aktif=1', (e.eid,)).fetchall():
        m = Makna()
        m.mid = rowm[0]
        m.nilai = expand_inline_singkatan(rowm[1])
        m.kelas = rowm[2]
        m.bahasa = rowm[3]
        m.bidang = rowm[4]
        m.ilmiah = rowm[5]
        m.kimia = rowm[6]
        m.is_ki = bool(rowm[7])
        m.is_kp = bool(rowm[8])
        m.is_akr = bool(rowm[9])
        if rowm[10]: m.ragams.append(rowm[10])
        if rowm[11]: m.ragams.append(rowm[11])
        e.maknas.append(m)

        # get contoh
        if m.mid in mids_with_contoh:
            for rowc in conn.execute('select cid, contoh from Contoh where mid=? and aktif=1', (m.mid,)).fetchall():
                c = Contoh()
                c.cid = rowc[0]
                c.nilai = expand_inline_singkatan(rowc[1])
                m.contohs.append(c)

    # rujuk
    e.jenis_rujuk = row[2]
    e.entri_rujuk = row[3]
    e.induk = row[4]

    # more fields
    e.silabel = row[5]
    e.jenis = row[6]
    e.entri_var = row[7]
    e.lafal = row[8]

    all_entries.append(e)

    # done
    index_entri_eid[e.eid] = e
    index_entri_nilai[e.nilai] = e

    # insert to acu and reverse ref too
    c = canonize(e.nilai)
    if c not in index_acu_nilai:
        a = Acu()
        all_acus.append(a)
        index_acu_nilai[c] = a
    else:
        a = index_acu_nilai[c]
    a.nilai = c
    a.entries.append(e)
    e.acu = a

# sort acu and give aid to them
all_acus.sort(key=lambda a: a.nilai)
for i, acu in enumerate(all_acus):
    acu.aid = i + 1

# make each Entri.entri_rujuk to point to the object
for e in all_entries:
    er = e.entri_rujuk
    if er:
        er2 = index_entri_nilai.get(er)
        if er2:
            e.acu_rujuks.append(er2.acu)
        else:
            ar2 = index_acu_nilai.get(er)
            if ar2:
                e.acu_rujuks.append(ar2)
            else:
                # coba huruf kecil dan buang angka dalam kurung
                er = re.sub(r' \(\d+\)', '', er).lower().strip()
                ar2 = index_acu_nilai.get(er)
                if ar2:
                    e.acu_rujuks.append(ar2)
                else:
                    # coba split di ';'
                    for er in re.split(r'\s*;\s*', er):
                        er2 = index_entri_nilai.get(er)
                        if er2:
                            e.acu_rujuks.append(er2.acu)
                        else:
                            er = re.sub(r' \(\d+\)', '', er).lower().strip()
                            ar2 = index_acu_nilai.get(er)
                            if ar2:
                                e.acu_rujuks.append(ar2)
                            else:
                                logging.warning("{} (jenis_rujuk {}): entri_rujuk or acu '{}' not found".format(e, e.jenis_rujuk, er))

# make each Entri.induk to point to the object
for e in all_entries:
    if e.induk:
        induk = index_entri_eid[e.induk]
        e.induk = induk
        induk.anaks.append(e)

for e in all_entries:
    if not e.maknas and not e.induk and not e.anaks and not e.jenis_rujuk and not e.entri_rujuk:
        logging.warning("{} has no makna and no induk/anaks and no valid entri_rujuk".format(e))

# read all kategoris
for row in conn.execute('select jenis, katid, kategori, urutan from Kategori where aktif=1').fetchall():
    k = Kategori()
    k.jenis = row[0]
    k.nilai = row[1]
    k.desc = row[2] or row[1]
    k.urutan = row[3]
    all_kategoris.append(k)

print('entry count:', len(all_entries))
print('acu count:', len(all_acus))
print('kategori count:', len(all_kategoris))


def varint_to_bytearray(buf: bytearray, val: int):
    if val > 0xffffff:
        raise ValueError('too long: {}'.format(val))
    elif val > 0x1ffff:  # (4 bytes)
        buf.append(0xfa)
        buf.append((val & 0xff0000) >> 16)
        buf.append((val & 0xff00) >> 8)
        buf.append(val & 0x00ff)
    elif val > 0xffff:  # 0x10000 to 0x1ffff (3 bytes)
        buf.append(0xfb)
        buf.append((val & 0xff00) >> 8)
        buf.append(val & 0x00ff)
    elif val > 0x1ff:  # 0x0200 to 0xffff (3 bytes)
        buf.append(0xfc)
        buf.append((val & 0xff00) >> 8)
        buf.append(val & 0x00ff)
    elif val > 0xff:  # 0x100 to 0x1ff (2 bytes)
        buf.append(0xfd)
        buf.append(val & 0xff)
    elif val > 0xef:  # 0xf0 to 0xff (2 bytes)
        buf.append(0xfe)
        buf.append(val)
    else:  # 0x00 to 0xef (1 byte)
        buf.append(val)


def write_varint(fo, val: int):
    b = bytearray()
    varint_to_bytearray(b, val)
    fo.write(b)


def write_text(fo, t: str):
    s = bytes(t, 'utf8')
    b = bytearray()
    varint_to_bytearray(b, len(s))
    fo.write(b)
    fo.write(s)


class Descml:
    def __init__(self):
        self.buf = bytearray()
        self.text_mode = False

    def varint(self, l):
        if self.text_mode:
            self.buf += bytes('({})'.format(l), 'utf8')
        else:
            varint_to_bytearray(self.buf, l)

    def text(self, t: str):
        if self.text_mode:
            self.buf += bytes(t, 'utf8')
        else:
            self.esc_text(0, t)

    def eof(self):
        self.esc_null(0xff)

    def esc_text(self, code: int, t: str):
        if self.text_mode:
            self.buf += bytes('[{}:{}]'.format(code, t), 'utf8')
        else:
            self.buf.append(code)
            s = bytes(t, 'utf8')
            self.varint(len(s))
            self.buf += s

    def esc_null(self, code: int):
        if self.text_mode:
            self.buf += bytes('[{}]'.format(code), 'utf8')
        else:
            self.buf.append(code)

    def esc_uint(self, code: int, val: int):
        if self.text_mode:
            self.buf += bytes('[{}:{}]'.format(code, val), 'utf8')
        else:
            self.buf.append(code)
            self.varint(val)


# must keep updated with client
CODE_ENTRI = 1  # text
CODE_LAFAL = 2  # text
CODE_SILABEL = 3  # text
CODE_ENTRI_VAR = 4  # text
CODE_UNGKAPAN = 5  # text
CODE_ANAK_varian = 10  # null
CODE_ANAK_dasar = 11  # null
CODE_ANAK_gabungan = 12  # null
CODE_ANAK_berimbuhan = 13  # null
CODE_ANAK_peribahasa = 14  # null
CODE_ANAK_idiom = 15  # null
CODE_KELAS = 20  # text
CODE_BAHASA = 21  # text
CODE_BIDANG = 22  # text
CODE_ILMIAH = 23  # text
CODE_KIMIA = 24  # text
CODE_RAGAM = 25  # text
CODE_ki = 30  # null
CODE_kp = 31  # null
CODE_akr = 32  # null
CODE_ukp = 33  # null
CODE_LINK_ACU = 40  # int
CODE_LINK_INDUK = 41  # int
CODE_LINK_NOT_FOUND = 42  # text
CODE_CONTOH = 50  # text
CODE_BOLD = 60  # text
CODE_ITALIC = 61  # text
CODE_SUB = 62  # text
CODE_SUP = 63  # text
CODE_KIMIA_SUB = 74  # text

# Hardcoded things (akan disyuh di masa depan)
HARDCODED_NO_CACINGIN = frozenset({
    "aspirat",
    "cadel",
    "ciri pembeda",
    "diafon",
    "labial",
    "labiovelar",
    "likuida",
    "notasi fonetis",
    "pelah",
    "semivokal",
    "bersuara",
    "uvular",
    "varian",
    "vibran",
    "vokal", "vokal tinggi", "diakritik",
    "vokal bawah", "vokal belakang", "vokal depan", "vokal hampar", "vokal tegang",
    "distribusi komplementer", "lenis",
    "kurung besar",
})

HARDCODED_NO_CACINGIN_PASSED = set()  # modifyable


def kenali_tag(d: Descml, s: str, tags: list, codes: list):
    pos = 0
    cat = '|'.join(tags)
    for m in re.finditer(r'<(' + cat + r')>(.*?)</\1>', s):
        d.text(s[pos:m.start(0)])
        tag = m.group(1)
        code = codes[tags.index(tag)]
        d.esc_text(code, m.group(2))
        pos = m.end(0)

    d.text(s[pos:])


def write_anaks(d: Descml, entri: Entri, allowed_anak, text_before: str):
    has_written = False

    by_code = {}

    for anak in sorted((anak for anak in entri.anaks if allowed_anak(anak)), key=lambda anak: anak.jenis):
        new_code = globals()['CODE_ANAK_{}'.format(anak.jenis)]
        per_code = by_code.get(new_code, [])
        per_code.append(anak.acu)
        by_code[new_code] = per_code

    for acus in by_code.values():
        acus.sort(key=lambda acu: acu.nilai)

    for code, acus in by_code.items():
        if text_before: d.text(text_before)
        d.esc_null(code)
        for i, acu in enumerate(acus):
            if i != 0: d.text('; ')
            d.esc_uint(CODE_LINK_ACU, acu.aid)

    return has_written


def render_acu(acu):
    d = Descml()
    d.text_mode = False

    first_entri = True
    for entri in acu.entries:
        if entri.induk and entri.induk.acu.aid == entri.acu.aid and not entri.maknas:
            logging.warning('skipping {} because induk refers to itself and has no makna'.format(entri))
            continue

        if not first_entri:
            d.text('\n\n')
        first_entri = False

        if entri.induk:
            d.esc_uint(CODE_LINK_INDUK, entri.induk.acu.aid)

        if entri.jenis == 'ukp':
            d.esc_text(CODE_UNGKAPAN, entri.nilai)
        else:
            if entri.silabel:
                d.esc_text(CODE_SILABEL, entri.silabel)
            else:
                d.esc_text(CODE_ENTRI, entri.nilai)

        if entri.lafal:
            d.text(' ')
            d.esc_text(CODE_LAFAL, entri.lafal)

        if entri.entri_var:
            d.text(' ')
            d.esc_text(CODE_ENTRI_VAR, entri.entri_var)

        if entri.jenis == 'ukp':
            d.text(' ')
            d.esc_null(CODE_ukp)

        d.text('\n')

        if entri.jenis_rujuk:
            if entri.jenis_rujuk == '→':
                d.text('bentuk tidak baku dari ')
            else:
                d.text(entri.jenis_rujuk + ' ')

            if entri.acu_rujuks:
                fst_acu_rujuk = True
                for acu_rujuk in entri.acu_rujuks:
                    if not fst_acu_rujuk: d.text('; ')
                    d.esc_uint(CODE_LINK_ACU, acu_rujuk.aid)
                    fst_acu_rujuk = False
                d.text('\n')
            else:
                # link ga ketemu, jadi manual saja dituliskan tanpa link
                d.esc_text(CODE_LINK_NOT_FOUND, entri.entri_rujuk)

        if write_anaks(d, entri, lambda anak: anak.jenis == 'varian', ''):
            d.text('\n')

        d.text('\n')

        makna_no = 0
        for makna in entri.maknas:
            makna_no += 1
            still_empty = True

            if makna_no > 1:
                d.text('\n\n')

            if len(entri.maknas) != 1:
                d.text('{}.'.format(makna_no))
                still_empty = False

            # sebelum makna utama
            if makna.kelas:
                if not still_empty: d.text(' ')
                still_empty = False
                d.esc_text(CODE_KELAS, makna.kelas)

            if makna.bahasa:
                if not still_empty: d.text(' ')
                still_empty = False
                d.esc_text(CODE_BAHASA, makna.bahasa)

            if makna.bidang:
                if not still_empty: d.text(' ')
                still_empty = False
                d.esc_text(CODE_BIDANG, makna.bidang)

            if makna.is_ki:
                if not still_empty: d.text(' ')
                still_empty = False
                d.esc_null(CODE_ki)

            if makna.is_kp:
                if not still_empty: d.text(' ')
                still_empty = False
                d.esc_null(CODE_kp)

            if makna.is_akr:
                if not still_empty: d.text(' ')
                still_empty = False
                d.esc_null(CODE_akr)

            for ragam in makna.ragams:
                if not still_empty: d.text(' ')
                still_empty = False
                d.esc_text(CODE_RAGAM, ragam)

            # makna utama
            if not still_empty: d.text(' ')

            def cacingin(s, e_nilai, e_jenis):
                if e_nilai in HARDCODED_NO_CACINGIN:
                    HARDCODED_NO_CACINGIN_PASSED.add(e_nilai)
                    return s

                entri_nonum = re.sub(r' \(\d+\)', '', e_nilai)
                diganti = '[' + entri_nonum + ']'

                need_warning = False
                if e_jenis == 'dasar':
                    ganti = '--'
                elif e_jenis == 'berimbuhan':
                    ganti = '~'
                else:
                    ganti = '--'
                    need_warning = True

                s2 = s.replace(diganti, ganti)
                if s2 != s and need_warning:
                    logging.warning('{}: makna or contoh contains [entri] but entri.jenis is {}'.format(e_nilai, e_jenis))
                s = s2

                # coba panjangin match
                # contoh: '[kata] dasar' menjadi: '[kata dasar]'
                pos = 0
                while 1:
                    pos = s.find('[', pos)
                    if pos == -1: break

                    pos2 = s.find(']', pos)
                    if pos2 == -1:
                        logging.warning('{}: no closing bracket: {}'.format(e_nilai, diganti, s))
                        break

                    pos3 = (s[:pos2] + s[pos2 + 1:]).lower().find(entri_nonum.lower(), pos + 1)
                    if pos3 != pos + 1:
                        logging.warning('{} -> {}: makna or contoh still contain brackets that could not be extended: {}'.format(e_nilai, diganti, s))
                        # force replace!
                        s = s[:pos] + ganti + s[pos2 + 1:]
                        pos = pos2 + 1  # try to find next '['
                        continue

                    s = s[:pos] + ganti + s[pos + 1 + len(entri_nonum) + 1:]
                    pos += len(ganti)

                return s

            m_nilai = cacingin(makna.nilai, entri.nilai, entri.jenis)

            kenali_tag(d, m_nilai, ['b', 'i', 'sub', 'sup'], [CODE_BOLD, CODE_ITALIC, CODE_SUB, CODE_SUP])

            # sesudah makna utama
            if makna.ilmiah:
                d.text('; ')
                d.esc_text(CODE_ILMIAH, makna.ilmiah)

            if makna.kimia:
                d.text('; ')

                def kimia_sub(s):
                    start = 0
                    while 1:
                        pos = s.find('<sub>', start)
                        if pos == -1:
                            break

                        pos2 = s.find('</sub>', pos)
                        if pos2 == -1:
                            break

                        d.esc_text(CODE_KIMIA, s[start:pos])
                        d.esc_text(CODE_KIMIA_SUB, s[pos+5:pos2])
                        start = pos2 + 6
                    d.esc_text(CODE_KIMIA, s[start:])

                kimia_sub(makna.kimia)

            if makna.contohs:
                d.text(': ')
                fst = True
                for contoh in makna.contohs:
                    if not fst:
                        d.text('; ')
                    c_nilai = cacingin(contoh.nilai, entri.nilai, entri.jenis)
                    d.esc_text(CODE_CONTOH, c_nilai)
                    fst = False

        write_anaks(d, entri, lambda anak: anak.jenis != 'varian', '\n\n')

    d.eof()
    return d.buf


def main():
    base_out_dir = '../android/app/src/main/assets/dictdata'

    for fn in os.listdir(base_out_dir):
        if fn.startswith('acu_desc_') or fn.startswith('kat_'):
            os.unlink('{}/{}'.format(base_out_dir, fn))

    acu_offlens = []

    file_no = 0
    fo = None
    to_encrypt_fns = []
    for acu in all_acus:
        if fo is None:
            fn = '{}/acu_desc_{}'.format(base_out_dir, file_no)
            to_encrypt_fns.append(fn)
            fo = open(fn + '.txt', 'wb')

        b = render_acu(acu)
        off = fo.tell()
        # fo.write(bytes(acu.nilai, 'utf8'))
        # fo.write(bytes('\n', 'utf8'))
        fo.write(b)
        # fo.write(bytes('\n', 'utf8'))
        lenn = fo.tell() - off
        acu_offlens.append((file_no, off, lenn))

        if off > 950000:
            fo.close()
            fo = None
            file_no += 1

    fo.close()
    fo = None

    for to_encrypt_fn in to_encrypt_fns:
        subprocess.call(['zopfli', to_encrypt_fn + '.txt'])
        # subprocess.call(['gzip', '-k', to_encrypt_fn + '.txt'])
        subprocess.call(['salsa20', '-p', to_encrypt_fn + '.txt.gz', to_encrypt_fn + '.s', os.environ['ENC_KEY_FULL32BYTE'] + os.environ['ENC_KEY_IV']])
        os.unlink(to_encrypt_fn + '.txt.gz')
        os.unlink(to_encrypt_fn + '.txt')

    if fo is not None:
        fo.close()

    for e_nilai in HARDCODED_NO_CACINGIN - HARDCODED_NO_CACINGIN_PASSED:
        logging.warning('HARDCODED_NO_CACINGIN not used: {}'.format(e_nilai))

    with open('{}/acu_nilai.txt'.format(base_out_dir), 'wb') as fo:
        # length first
        b = bytearray()
        varint_to_bytearray(b, len(all_acus))
        fo.write(b)

        for acu in all_acus:
            s = bytes(acu.nilai, 'utf8')
            fo.write(bytes([len(s)]))
            fo.write(s)

    with open('{}/acu_offlens.txt'.format(base_out_dir), 'wb') as fo:
        # length first
        b = bytearray()
        varint_to_bytearray(b, len(acu_offlens))
        fo.write(b)

        last_file_no = -1

        for offlen in acu_offlens:
            b = bytearray()

            file_no = offlen[0]
            if file_no != last_file_no:
                last_file_no = file_no
                varint_to_bytearray(b, 0xffff)  # mark: change file_no

            varint_to_bytearray(b, offlen[2])

            fo.write(b)

    # kategori list per facet
    def facetize(fname, fextractor, fhas, is_jenis=False):
        fmap = {}
        for e in all_entries:
            if is_jenis:  # special case
                if fhas(e):
                    for fvalue in fextractor(e):
                        ls = fmap.get(fvalue)
                        if not ls: fmap[fvalue] = ls = []
                        ls.append(e.acu)
            else:
                for m in e.maknas:
                    if fhas(m):
                        for fvalue in fextractor(m):
                            ls = fmap.get(fvalue)
                            if not ls: fmap[fvalue] = ls = []
                            ls.append(e.acu)
        for fvalue, acus in fmap.items():
            acus = sorted(set(acus))
            print(u'Facet {}: {} ({} acus)'.format(fname, fvalue, len(acus)))
            with open('{}/kat_{}_{}.txt'.format(base_out_dir, fname, fvalue), 'wb') as fo:
                # length first
                write_varint(fo, len(acus))

                for a in acus:
                    write_varint(fo, a.aid)

        with open('{}/kat_index_{}.txt'.format(base_out_dir, fname), 'wb') as fo:
            filtered = sorted(list(filter(lambda k: k.jenis == fname, all_kategoris)), key=lambda k: (k.urutan, k.desc.lower()))

            # length first
            write_varint(fo, len(filtered))

            for k in filtered:
                write_text(fo, k.nilai)
                write_text(fo, k.desc)

    facetize('bahasa', lambda m: [m.bahasa], lambda m: m.bahasa)
    facetize('bidang', lambda m: [m.bidang], lambda m: m.bidang)
    facetize('kelas', lambda m: [m.kelas], lambda m: m.kelas)
    facetize('ragam', lambda m: m.ragams, lambda m: m.ragams)
    facetize('jenis', lambda e: [e.jenis], lambda e: e.jenis, is_jenis=True)


def cari_peribahasa():
    peri = sorted([e for e in all_entries if e.jenis == 'peribahasa'], key=lambda e: e.nilai)
    for p in peri: print(p)

    def lev(s, t):
        ns = len(s) + 1
        nt = len(t) + 1
        m = [[0] * nt for _ in range(ns)]
        for i in range(ns): m[i][0] = i
        for j in range(nt): m[0][j] = j
        for i in range(1, ns):
            for j in range(1, nt):
                if s[i - 1] == t[j - 1]:
                    m[i][j] = m[i - 1][j - 1]
                else:
                    m[i][j] = min([m[i - 1][j], m[i][j - 1], m[i - 1][j - 1]]) + 1
        return m[ns - 1][nt - 1]

    def cln(s):
        return ''.join(c for c in list(s) if 'A' <= c <= 'Z' or 'a' <= c <= 'z')

    for i, p1 in enumerate(peri):
        for p2 in peri[i + 1:]:
            c1 = cln(p1.nilai)
            c2 = cln(p2.nilai)
            d = lev(c1, c2)
            f = float(d) / min(len(c1), len(c2))
            if f < 0.3:
                print("{} {:.2} {} {}".format(d, f, p1, p2))


main()
# cari_peribahasa()
