import logging
import os
import re
import sqlite3


class Entri:
    def __init__(self):
        self.eid = None
        self.nilai = None
        self.maknas = []
        self.jenis_rujuk = None
        self.entri_rujuk = None
        self.induk = None
        self.anaks = []
        self.silabel = None
        self.acu = None
        self.jenis = None
        self.entri_var = None

    def __repr__(self):
        return u"Entri<{} '{}' {}>".format(self.eid, self.nilai, self.maknas)


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


index_acu_nilai = {}
index_entri_eid = {}
index_entri_nilai = {}
all_acus = []
all_entries = []


def canonize(s: str):
    s = s.lower()
    # TODO diacritics
    m = re.match(r'(.*?)\s*\(\d+\)', s)
    if m:
        return m.group(1)
    return s


conn = sqlite3.connect('in/kbbi4v4_2.db')
mids_with_contoh = set()
for row in conn.execute('select distinct mid from Contoh where aktif=1').fetchall():
    mids_with_contoh.add(row[0])

print('mids_with_contoh count:', len(mids_with_contoh))

for row in conn.execute('select eid, entri, jenis_rujuk, entri_rujuk, induk, silabel, jenis, entri_var from Entri where aktif=1').fetchall():
    e = Entri()
    e.eid = row[0]
    e.nilai = row[1]

    # get makna
    for rowm in conn.execute('select mid, makna, kelas, bahasa, bidang, ilmiah, kimia, ki, kp, akr from Makna where eid=? and aktif=1', (e.eid,)).fetchall():
        m = Makna()
        m.mid = rowm[0]
        m.nilai = rowm[1]
        m.kelas = rowm[2]
        m.bahasa = rowm[3]
        m.bidang = rowm[4]
        m.ilmiah = rowm[5]
        m.kimia = rowm[6]
        m.is_ki = bool(rowm[7])
        m.is_kp = bool(rowm[8])
        m.is_akr = bool(rowm[9])
        e.maknas.append(m)

        # get contoh
        if m.mid in mids_with_contoh:
            for rowc in conn.execute('select cid, contoh from Contoh where mid=? and aktif=1', (m.mid,)).fetchall():
                c = Contoh()
                c.cid = rowc[0]
                c.nilai = rowc[1]
                m.contohs.append(c)

    # rujuk
    e.jenis_rujuk = row[2]
    e.entri_rujuk = row[3]
    e.induk = row[4]

    # more fields
    e.silabel = row[5]
    e.jenis = row[6]
    e.entri_var = row[7]

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
        e.entri_rujuk = index_entri_nilai.get(er)
        if not e.entri_rujuk:
            logging.warning("{} entri_rujuk '{}' not found".format(e, er))

# make each Entri.induk to point to the object
for e in all_entries:
    if e.induk:
        induk = index_entri_eid[e.induk]
        e.induk = induk
        induk.anaks.append(e)

for e in all_entries:
    if not e.maknas and not e.induk and not e.anaks and not e.jenis_rujuk and not e.entri_rujuk:
        logging.warning("{} has no makna and no induk/anaks and no valid entri_rujuk".format(e))

print('entry count:', len(all_entries))
print('acu count:', len(all_acus))


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
CODE_SILABEL = 2  # text
CODE_ENTRI_VAR = 4  # text
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
CODE_ki = 30  # null
CODE_kp = 31  # null
CODE_akr = 32  # null
CODE_LINK_ACU = 40  # int
CODE_LINK_INDUK = 41  # int
CODE_CONTOH = 50  # text


def render_acu(acu):
    d = Descml()
    d.text_mode = False
    for entri in acu.entries:
        if entri.induk:
            d.esc_uint(CODE_LINK_INDUK, entri.induk.acu.aid)

        d.esc_text(CODE_ENTRI, entri.nilai)

        if entri.entri_var:
            d.text(' ')
            d.esc_text(CODE_ENTRI_VAR, entri.entri_var)

        if entri.silabel:
            d.text(' ')
            d.esc_text(CODE_SILABEL, entri.silabel)

        d.text('\n')

        makna_no = 0

        if entri.jenis_rujuk and entri.entri_rujuk:
            d.text(entri.jenis_rujuk)
            d.text(' ')
            d.esc_uint(CODE_LINK_ACU, entri.entri_rujuk.acu.aid)
            d.text('\n')

        for makna in entri.maknas:
            makna_no += 1
            d.text('{}.'.format(makna_no))

            # sebelum makna utama
            if makna.kelas:
                d.text(' ')
                d.esc_text(CODE_KELAS, makna.kelas)

            if makna.bahasa:
                d.text(' ')
                d.esc_text(CODE_BAHASA, makna.bahasa)

            if makna.bidang:
                d.text(' ')
                d.esc_text(CODE_BIDANG, makna.bidang)

            if makna.is_ki:
                d.text(' ')
                d.esc_null(CODE_ki)

            if makna.is_kp:
                d.text(' ')
                d.esc_null(CODE_kp)

            if makna.is_akr:
                d.text(' ')
                d.esc_null(CODE_akr)

            # makna utama
            d.text(' {}'.format(makna.nilai))

            # sesudah makna utama
            if makna.ilmiah:
                d.text('; ')
                d.esc_text(CODE_ILMIAH, makna.ilmiah)

            if makna.kimia:
                d.text('; ')
                d.esc_text(CODE_KIMIA, makna.kimia)

            if makna.contohs:
                d.text(': ')
                fst = True
                for contoh in makna.contohs:
                    d.esc_text(CODE_CONTOH, contoh.nilai)
                    if not fst:
                        d.text('; ')
                    fst = False

            d.text('\n')

        code = 0
        fst = True
        for anak in sorted(entri.anaks, key=lambda anak: anak.jenis):
            new_code = globals()['CODE_ANAK_{}'.format(anak.jenis)]
            if new_code != code:
                fst = True
                code = new_code
                d.esc_null(code)
            if not fst:
                d.text('; ')
            d.esc_uint(CODE_LINK_ACU, anak.acu.aid)
            fst = False

        d.text('\n')

    d.eof()
    return d.buf


def main():
    base_out_dir = '../android/app/src/main/assets/dictdata'

    for fn in os.listdir(base_out_dir):
        if fn.startswith('acu_desc_'):
            os.unlink('{}/{}'.format(base_out_dir, fn))

    acu_offlens = []

    file_no = 0
    fo = None
    for acu in all_acus:
        if fo is None:
            fo = open('{}/acu_desc_{}.txt'.format(base_out_dir, file_no), 'wb')

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

    if fo is not None:
        fo.close()

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

main()

# bedakan anaks berdasarkan 'jenis' OK

# entri_var tampilkan begini: berambin (_atau_ berambin lutut) DI CLIENT

# parse <i>

# parse <sub> di kimia
