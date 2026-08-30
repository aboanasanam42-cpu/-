import os, sqlite3, threading, time, webbrowser, urllib.parse, json, shutil
from datetime import datetime
import tkinter as tk
from tkinter import ttk, messagebox, filedialog

APP_DIR = os.path.join(os.environ.get('APPDATA', os.path.expanduser('~')), 'MedicalAccounting')
BACKUP_DIR = os.path.join(APP_DIR, 'backups')
os.makedirs(BACKUP_DIR, exist_ok=True)
DB = os.path.join(APP_DIR, 'medical_accounting.db')

SCHEMA = '''
CREATE TABLE IF NOT EXISTS patients(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT,case_type TEXT,diagnosis TEXT,notes TEXT,created_at TEXT);
CREATE TABLE IF NOT EXISTS transactions(id INTEGER PRIMARY KEY AUTOINCREMENT,patient_id INTEGER,section TEXT,description TEXT,amount REAL DEFAULT 0,paid REAL DEFAULT 0,remaining REAL DEFAULT 0,doctor_share REAL DEFAULT 0,lab_share REAL DEFAULT 0,created_at TEXT,FOREIGN KEY(patient_id) REFERENCES patients(id));
CREATE TABLE IF NOT EXISTS expenses(id INTEGER PRIMARY KEY AUTOINCREMENT,category TEXT,description TEXT,amount REAL DEFAULT 0,created_at TEXT);
CREATE TABLE IF NOT EXISTS messages(id INTEGER PRIMARY KEY AUTOINCREMENT,patient_id INTEGER,phone TEXT,message TEXT,send_at TEXT,channel TEXT,status TEXT DEFAULT 'pending');
'''

class MedicalAccountingApp:
    def __init__(self, root):
        self.root = root
        self.root.title('المحاسب الطبي الشامل')
        self.root.geometry('1280x820')
        self.root.minsize(1050, 700)
        self.root.configure(bg='#eef3fb')
        self.db = sqlite3.connect(DB, check_same_thread=False)
        self.db.executescript(SCHEMA); self.db.commit()
        self.style = ttk.Style(); self.style.theme_use('clam')
        self.style.configure('TButton', font=('Segoe UI', 11), padding=8)
        self.style.configure('TLabel', font=('Segoe UI', 11))
        self.build_dashboard(); self.refresh_status(); self.backup()
        threading.Thread(target=self.scheduler, daemon=True).start()

    def build_dashboard(self):
        outer = tk.Frame(self.root, bg='#eef3fb'); outer.pack(fill='both', expand=True)
        header = tk.Frame(outer, bg='#eef3fb'); header.pack(fill='x', padx=34, pady=(20, 10))
        title = tk.Label(header, text='🩺  التطبيق المحاسبي الطبي الشامل', bg='#2e558f', fg='white',
                         font=('Segoe UI', 23, 'bold'), padx=28, pady=12, relief='flat')
        title.pack(anchor='center')
        tk.Label(header, text='نظام إدارة المرضى والحسابات والتقارير — نسخة Windows', bg='#eef3fb',
                 fg='#40516b', font=('Segoe UI', 11)).pack(pady=7)

        self.canvas = tk.Canvas(outer, bg='#eef3fb', highlightthickness=0)
        self.canvas.pack(fill='both', expand=True, padx=28)
        self.canvas.bind('<Configure>', self.draw_cards)
        self.card_defs = [
            ('الاستقبال','👩‍💼','#274b86',self.open_reception),
            ('الطبيب','🩺','#25b883',lambda:self.open_service('الطبيب')),
            ('المختبرات','🧪','#2d8ed5',lambda:self.open_service('المختبرات')),
            ('الأشعة والأجهزة الأخرى','🩻','#d5a247',lambda:self.open_service('الأشعة والأجهزة الأخرى')),
            ('الصيدلية','💊','#1aa8a3',lambda:self.open_service('الصيدلية')),
            ('الخرجيات العامة','🧾','#8062ad',self.open_expenses),
        ]
        self.utility_defs = [
            ('🔎  البحث السريع', self.open_search, '#2386bd'),
            ('🧮  الفواتير / التقارير / كشف حساب المريض', self.open_reports, '#22a8a0'),
            ('☁  الحفظ / النسخ الاحتياطية', self.open_backup, '#7785a0'),
            ('✉  الرسائل التلقائية', self.open_messages, '#294e88'),
            ('🖨  طباعة الفواتير', self.open_print, '#23b978'),
            ('📊  التقارير اليومية والأسبوعية والشهرية والسنوية', self.open_reports, '#805faf'),
        ]
        footer = tk.Frame(outer, bg='#eef3fb', bd=1, relief='solid')
        footer.pack(fill='x', padx=38, pady=(8, 18))
        self.status = tk.Label(footer, text='', bg='#eef3fb', fg='#40516b', font=('Segoe UI', 10), pady=8)
        self.status.pack()
        tk.Label(footer, text='مشارك فكرة التطبيق / محمد عبدالقوي سعيد الرميمة', bg='#eef3fb', fg='#1d3557', font=('Segoe UI', 11, 'bold')).pack(pady=(0,3))
        tk.Label(footer, text='تصميم وبرمجة الدكتور / مالك الرميمة', bg='#eef3fb', fg='#40516b', font=('Segoe UI', 10)).pack(pady=(0,8))

    def draw_round_rect(self, x1,y1,x2,y2,r=18,fill='white',outline='#dbe3ef',width=1):
        self.canvas.create_rectangle(x1+r,y1,x2-r,y2,fill=fill,outline='',width=0)
        self.canvas.create_rectangle(x1,y1+r,x2,y2-r,fill=fill,outline='',width=0)
        self.canvas.create_oval(x1,y1,x1+2*r,y1+2*r,fill=fill,outline='')
        self.canvas.create_oval(x2-2*r,y1,x2,y1+2*r,fill=fill,outline='')
        self.canvas.create_oval(x1,y2-2*r,x1+2*r,y2,fill=fill,outline='')
        self.canvas.create_oval(x2-2*r,y2-2*r,x2,y2,fill=fill,outline='')
        self.canvas.create_rectangle(x1+r,y1,x2-r,y1+2,width=0,fill=fill,outline='')
        self.canvas.create_rectangle(x1+r,y2-2,x2-r,y2,width=0,fill=fill,outline='')

    def draw_cards(self, event=None):
        self.canvas.delete('all')
        w = max(self.canvas.winfo_width(), 1000)
        gap=22; margin=18; card_w=(w-2*margin-2*gap)/3; card_h=190
        for i,(label,icon,color,cmd) in enumerate(self.card_defs):
            row,col=divmod(i,3); x=margin+col*(card_w+gap); y=18+row*(card_h+gap)
            self.draw_round_rect(x,y,x+card_w,y+card_h,22,'white')
            self.canvas.create_text(x+card_w/2,y+58,text=icon,font=('Segoe UI Emoji',42),fill='#17253a')
            self.canvas.create_text(x+card_w/2,y+113,text=label,font=('Segoe UI',20,'bold'),fill='#111827')
            self.canvas.create_rectangle(x+1,y+card_h-18,x+card_w-1,y+card_h-1,fill=color,outline=color)
            self.canvas.create_text(x+card_w/2,y+card_h-9,text='فتح القسم',font=('Segoe UI',9,'bold'),fill='white')
            self.canvas.create_rectangle(x,y,x+card_w,y+card_h,outline='',fill='',tags=(f'card{i}',))
            self.canvas.tag_bind(f'card{i}','<Button-1>',lambda e,c=cmd:c())
            self.canvas.tag_bind(f'card{i}','<Enter>',lambda e:self.canvas.configure(cursor='hand2'))
        base=2*(card_h+gap)+35; utility_h=70; uw=(w-2*margin-2*gap)/3
        for i,(label,cmd,color) in enumerate(self.utility_defs):
            row,col=divmod(i,3); x=margin+col*(uw+gap); y=base+row*(utility_h+14)
            self.draw_round_rect(x,y,x+uw,y+utility_h,16,'white')
            self.canvas.create_rectangle(x,y+utility_h-5,x+uw,y+utility_h,fill=color,outline=color)
            tag=f'utility{i}'
            self.canvas.create_text(x+uw/2,y+utility_h/2-2,text=label,font=('Segoe UI',12,'bold'),fill='#172033',tags=tag)
            self.canvas.tag_bind(tag,'<Button-1>',lambda e,c=cmd:c()); self.canvas.tag_bind(tag,'<Enter>',lambda e:self.canvas.configure(cursor='hand2'))

    def refresh_status(self):
        try:
            p=self.db.execute('SELECT COUNT(*) FROM patients').fetchone()[0]
            t=self.db.execute('SELECT COALESCE(SUM(amount),0),COALESCE(SUM(paid),0),COALESCE(SUM(remaining),0) FROM transactions').fetchone()
            self.status.config(text=f'المرضى: {p}    |    إجمالي المعالجات: {t[0]:.2f}    |    الواصل: {t[1]:.2f}    |    المتبقي: {t[2]:.2f}')
        except Exception: pass

    def win(self,title,w=900,h=650):
        top=tk.Toplevel(self.root); top.title(title); top.geometry(f'{w}x{h}'); top.minsize(700,450); top.configure(bg='#f5f7fb'); top.transient(self.root)
        tk.Label(top,text=title,bg='#2e558f',fg='white',font=('Segoe UI',19,'bold'),padx=20,pady=10).pack(fill='x',padx=18,pady=(15,10)); return top

    def entry(self,parent,label,row,var=None):
        if var is None: var=tk.StringVar()
        tk.Label(parent,text=label,bg=parent.cget('bg'),font=('Segoe UI',11)).grid(row=row,column=0,padx=12,pady=7,sticky='e')
        e=ttk.Entry(parent,textvariable=var,width=55); e.grid(row=row,column=1,padx=12,pady=7,sticky='ew'); return var

    def open_reception(self):
        top=self.win('الاستقبال — تسجيل مريض وعملية جديدة')
        f=tk.Frame(top,bg='#f5f7fb',padx=30,pady=10); f.pack(fill='both',expand=True); f.columnconfigure(1,weight=1)
        vals={k:tk.StringVar() for k in ['name','phone','case','diagnosis','notes','amount','paid']}
        labels=[('name','اسم المريض'),('phone','رقم الهاتف'),('case','نوع الحالة'),('diagnosis','التشخيص'),('notes','ملاحظات'),('amount','إجمالي المعالجة'),('paid','الواصل')]
        for r,(k,l) in enumerate(labels): self.entry(f,l,r,vals[k])
        def save():
            if not vals['name'].get().strip(): return messagebox.showerror('خطأ','اسم المريض مطلوب',parent=top)
            try:a=float(vals['amount'].get() or 0); p=float(vals['paid'].get() or 0)
            except: return messagebox.showerror('خطأ','المبلغ والواصل يجب أن يكونا أرقاماً',parent=top)
            now=datetime.now().isoformat(timespec='seconds'); cur=self.db.execute('INSERT INTO patients(name,phone,case_type,diagnosis,notes,created_at) VALUES(?,?,?,?,?,?)',(vals['name'].get().strip(),vals['phone'].get().strip(),vals['case'].get(),vals['diagnosis'].get(),vals['notes'].get(),now)); pid=cur.lastrowid
            self.db.execute('INSERT INTO transactions(patient_id,section,description,amount,paid,remaining,created_at) VALUES(?,?,?,?,?,?,?)',(pid,'الاستقبال','معاينة / معالجة',a,p,a-p,now)); self.db.commit(); self.backup(); self.refresh_status(); messagebox.showinfo('تم','تم حفظ المريض والعملية بنجاح',parent=top); top.destroy()
        ttk.Button(f,text='حفظ المريض والعملية',command=save).grid(row=7,column=1,pady=18,sticky='e')

    def open_service(self,section):
        top=self.win(section+' — تسجيل عملية')
        f=tk.Frame(top,bg='#f5f7fb',padx=30,pady=12); f.pack(fill='both',expand=True); f.columnconfigure(1,weight=1)
        name,desc,amount,paid=[tk.StringVar() for _ in range(4)]
        for r,(l,v) in enumerate([('اسم المريض',name),('الخدمة / الوصف',desc),('المبلغ',amount),('الواصل',paid)]): self.entry(f,l,r,v)
        def save():
            row=self.db.execute('SELECT id FROM patients WHERE name LIKE ? ORDER BY id DESC LIMIT 1',('%'+name.get().strip()+'%',)).fetchone()
            if not row:return messagebox.showerror('خطأ','لم يتم العثور على المريض. سجله أولاً من الاستقبال.',parent=top)
            try:a=float(amount.get() or 0); p=float(paid.get() or 0)
            except:return messagebox.showerror('خطأ','المبلغ والواصل يجب أن يكونا أرقاماً',parent=top)
            self.db.execute('INSERT INTO transactions(patient_id,section,description,amount,paid,remaining,created_at) VALUES(?,?,?,?,?,?,?)',(row[0],section,desc.get(),a,p,a-p,datetime.now().isoformat(timespec='seconds'))); self.db.commit(); self.backup(); self.refresh_status(); messagebox.showinfo('تم','تم تسجيل العملية',parent=top); top.destroy()
        ttk.Button(f,text='حفظ العملية',command=save).grid(row=4,column=1,pady=18,sticky='e')

    def open_expenses(self):
        top=self.win('الخرجيات العامة'); f=tk.Frame(top,bg='#f5f7fb',padx=30,pady=15); f.pack(fill='both',expand=True); f.columnconfigure(1,weight=1)
        c,d,a=[tk.StringVar() for _ in range(3)]
        for r,(l,v) in enumerate([('نوع الخرجية',c),('الوصف',d),('المبلغ',a)]): self.entry(f,l,r,v)
        def save():
            try:x=float(a.get() or 0)
            except:return messagebox.showerror('خطأ','المبلغ غير صحيح',parent=top)
            self.db.execute('INSERT INTO expenses(category,description,amount,created_at) VALUES(?,?,?,?)',(c.get(),d.get(),x,datetime.now().isoformat(timespec='seconds'))); self.db.commit(); self.backup(); self.refresh_status(); messagebox.showinfo('تم','تم تسجيل الخرجية',parent=top); top.destroy()
        ttk.Button(f,text='حفظ الخرجية',command=save).grid(row=3,column=1,pady=18,sticky='e')

    def open_search(self):
        top=self.win('البحث السريع — البحث في كامل البرنامج',1050,650)
        bar=tk.Frame(top,bg='#f5f7fb',padx=20,pady=8); bar.pack(fill='x'); q=tk.StringVar(); ttk.Entry(bar,textvariable=q,width=70).pack(side='right',padx=8); ttk.Button(bar,text='بحث',command=lambda:self.do_search(q.get(),tree)).pack(side='right')
        cols=('id','name','phone','case','diagnosis','section','description','amount','paid','remaining','date'); tree=ttk.Treeview(top,columns=cols,show='headings')
        heads=['ID','المريض','الهاتف','الحالة','التشخيص','القسم','الوصف','المبلغ','الواصل','الباقي','التاريخ']
        for c,h in zip(cols,heads): tree.heading(c,text=h); tree.column(c,width=90,anchor='center')
        tree.pack(fill='both',expand=True,padx=20,pady=10)
        self.do_search('',tree)

    def do_search(self,q,tree):
        for x in tree.get_children():tree.delete(x)
        like='%'+q.strip()+'%'
        rows=self.db.execute('SELECT p.id,p.name,p.phone,p.case_type,p.diagnosis,t.section,t.description,t.amount,t.paid,t.remaining,t.created_at FROM patients p LEFT JOIN transactions t ON p.id=t.patient_id WHERE p.name LIKE ? OR p.phone LIKE ? OR p.case_type LIKE ? OR p.diagnosis LIKE ? OR t.section LIKE ? OR t.description LIKE ? ORDER BY p.id DESC',(like,like,like,like,like,like)).fetchall()
        for r in rows:tree.insert('', 'end', values=r)

    def open_reports(self):
        top=self.win('الفواتير والتقارير وكشف حساب المرضى',1050,680)
        f=tk.Frame(top,bg='#f5f7fb',padx=22,pady=10); f.pack(fill='x'); name=tk.StringVar(); period=tk.StringVar(value='اليومي')
        self.entry(f,'اسم المريض (فارغ = التقرير العام)',0,name); ttk.Combobox(f,textvariable=period,values=['اليومي','الأسبوعي','الشهري','السنوي'],state='readonly',width=18).grid(row=1,column=1,sticky='e',pady=7)
        btn=tk.Frame(f,bg='#f5f7fb'); btn.grid(row=2,column=1,sticky='e',pady=8)
        ttk.Button(btn,text='إنشاء PDF',command=lambda:self.export_pdf(name.get(),period.get(),top)).pack(side='right',padx=4); ttk.Button(btn,text='إنشاء Excel',command=lambda:self.export_xlsx(name.get(),period.get(),top)).pack(side='right',padx=4)
        ttk.Button(btn,text='كشف حساب المريض',command=lambda:self.patient_statement(name.get(),top)).pack(side='right',padx=4)
        tree=ttk.Treeview(top,columns=('name','section','desc','amount','paid','remain','date'),show='headings')
        for c,h in zip(tree['columns'],['المريض','القسم','الوصف','المبلغ','الواصل','الباقي','التاريخ']):tree.heading(c,text=h)
        tree.pack(fill='both',expand=True,padx=20,pady=10)
        def refresh():
            for x in tree.get_children():tree.delete(x)
            for r in self.get_rows(name.get()):tree.insert('', 'end', values=r)
        ttk.Button(top,text='تحديث البيانات',command=refresh).pack(pady=8); refresh()

    def get_rows(self,name=''):
        if name.strip():return self.db.execute('SELECT p.name,p.phone,t.section,t.description,t.amount,t.paid,t.remaining,t.created_at FROM patients p JOIN transactions t ON p.id=t.patient_id WHERE p.name LIKE ? ORDER BY t.id DESC',('%'+name.strip()+'%',)).fetchall()
        return self.db.execute('SELECT p.name,p.phone,t.section,t.description,t.amount,t.paid,t.remaining,t.created_at FROM patients p JOIN transactions t ON p.id=t.patient_id ORDER BY t.id DESC').fetchall()

    def export_pdf(self,name,period,parent=None):
        path=filedialog.asksaveasfilename(parent=parent,defaultextension='.pdf',filetypes=[('PDF','*.pdf')],initialfile=f'تقرير_طبي_{period}.pdf')
        if not path:return
        try:
            from reportlab.lib.pagesizes import A4
            from reportlab.pdfgen import canvas
            c=canvas.Canvas(path,pagesize=A4); w,h=A4; y=h-45
            c.setFont('Helvetica-Bold',15); c.drawString(40,y,'Comprehensive Medical Accounting'); y-=25
            c.setFont('Helvetica',10); c.drawString(40,y,f'Period: {period}   Patient: {name or "All"}'); y-=25
            total=paid=remain=0
            for r in self.get_rows(name):
                total+=float(r[4]); paid+=float(r[5]); remain+=float(r[6]); line=f'{r[0]} | {r[2]} | {r[3]} | {r[4]:.2f} | {r[5]:.2f} | {r[6]:.2f}'
                c.drawString(35,y,line[:120]); y-=16
                if y<50:c.showPage(); y=h-45; c.setFont('Helvetica',10)
            c.drawString(35,max(y,50),f'TOTAL: {total:.2f}   PAID: {paid:.2f}   REMAINING: {remain:.2f}'); c.save(); messagebox.showinfo('تم','تم حفظ ملف PDF في:\n'+path,parent=parent)
        except Exception as e:messagebox.showerror('PDF',str(e),parent=parent)

    def export_xlsx(self,name,period,parent=None):
        path=filedialog.asksaveasfilename(parent=parent,defaultextension='.xlsx',filetypes=[('Excel','*.xlsx')],initialfile=f'تقرير_طبي_{period}.xlsx')
        if not path:return
        try:
            from openpyxl import Workbook
            wb=Workbook(); ws=wb.active; ws.title='Medical Report'; ws.append(['Patient','Phone','Section','Description','Amount','Paid','Remaining','Date'])
            for r in self.get_rows(name):ws.append(list(r))
            wb.save(path); messagebox.showinfo('تم','تم حفظ ملف Excel في:\n'+path,parent=parent)
        except Exception as e:messagebox.showerror('Excel',str(e),parent=parent)

    def patient_statement(self,name,parent=None):
        if not name.strip():return messagebox.showwarning('كشف الحساب','اكتب اسم المريض أولاً',parent=parent)
        self.export_pdf(name,'كشف حساب',parent)

    def open_backup(self):
        top=self.win('الحفظ والنسخ الاحتياطية',820,500)
        f=tk.Frame(top,bg='#f5f7fb',padx=25,pady=20); f.pack(fill='both',expand=True)
        tk.Label(f,text='يتم الحفظ تلقائياً بعد العمليات، ويمكنك إنشاء نسخة يدوية للهاتف/الكمبيوتر.',bg='#f5f7fb',fg='#334155',font=('Segoe UI',13),wraplength=700).pack(pady=15)
        ttk.Button(f,text='حفظ نسخة احتياطية الآن',command=lambda:(self.backup(),messagebox.showinfo('تم','تم إنشاء النسخة الاحتياطية',parent=top))).pack(pady=8)
        ttk.Button(f,text='تصدير قاعدة البيانات',command=self.export_db).pack(pady=8)
        ttk.Button(f,text='فتح مجلد النسخ الاحتياطية',command=lambda:os.startfile(BACKUP_DIR)).pack(pady=8)
        ttk.Button(f,text='إنشاء PDF للبيانات',command=lambda:self.export_pdf('','كامل',top)).pack(pady=8)
        ttk.Button(f,text='إنشاء Excel للبيانات',command=lambda:self.export_xlsx('','كامل',top)).pack(pady=8)

    def export_db(self):
        path=filedialog.asksaveasfilename(defaultextension='.db',filetypes=[('Database','*.db')],initialfile='medical_accounting_backup.db')
        if path:shutil.copy2(DB,path); messagebox.showinfo('تم','تم تصدير قاعدة البيانات')

    def backup(self):
        try:
            ts=datetime.now().strftime('%Y%m%d_%H%M%S'); dst=os.path.join(BACKUP_DIR,f'medical_accounting_{ts}.db'); shutil.copy2(DB,dst)
            data={'created_at':datetime.now().isoformat(),'patients':[dict(zip(['id','name','phone','case_type','diagnosis','notes','created_at'],r)) for r in self.db.execute('SELECT * FROM patients')], 'transactions':[dict(zip(['id','patient_id','section','description','amount','paid','remaining','doctor_share','lab_share','created_at'],r)) for r in self.db.execute('SELECT * FROM transactions')], 'expenses':[dict(zip(['id','category','description','amount','created_at'],r)) for r in self.db.execute('SELECT * FROM expenses')], 'messages':[dict(zip(['id','patient_id','phone','message','send_at','channel','status'],r)) for r in self.db.execute('SELECT * FROM messages')]}
            with open(os.path.join(BACKUP_DIR,f'medical_accounting_{ts}.json'),'w',encoding='utf-8') as f:json.dump(data,f,ensure_ascii=False,indent=2)
        except Exception:pass

    def open_messages(self):
        top=self.win('الرسائل التلقائية للأمراض والمرتبطين بمجال العمل',900,650)
        f=tk.Frame(top,bg='#f5f7fb',padx=25,pady=10); f.pack(fill='both',expand=True); f.columnconfigure(1,weight=1)
        phone,when,channel=[tk.StringVar() for _ in range(3)]; channel.set('WhatsApp'); when.set(datetime.now().strftime('%Y-%m-%d %H:%M'))
        self.entry(f,'رقم الهاتف',0,phone); self.entry(f,'تاريخ ووقت الإرسال YYYY-MM-DD HH:MM',1,when)
        tk.Label(f,text='قناة الإرسال',bg='#f5f7fb').grid(row=2,column=0,padx=12,pady=7,sticky='e'); ttk.Combobox(f,textvariable=channel,values=['WhatsApp','SMS'],state='readonly',width=20).grid(row=2,column=1,sticky='e')
        tk.Label(f,text='نص الرسالة',bg='#f5f7fb').grid(row=3,column=0,padx=12,pady=7,sticky='ne'); text=tk.Text(f,height=9,width=70,font=('Segoe UI',11)); text.grid(row=3,column=1,pady=7,sticky='ew')
        ttk.Button(f,text='جدولة الرسالة',command=lambda:self.schedule_message(phone.get(),when.get(),channel.get(),text.get('1.0','end').strip(),top)).grid(row=4,column=1,pady=12,sticky='e')
        tk.Label(f,text='يمكن إضافة عمليات متعددة كل واحدة بتاريخ ووقت مستقل. WhatsApp/SMS يفتح قناة الإرسال عند الموعد ولا يتجاوز صلاحيات النظام.',bg='#f5f7fb',fg='#64748b',wraplength=700).grid(row=5,column=0,columnspan=2,pady=15)
        tree=ttk.Treeview(f,columns=('phone','when','channel','status'),show='headings',height=8)
        for c,h in zip(tree['columns'],['الهاتف','الموعد','القناة','الحالة']):tree.heading(c,text=h)
        tree.grid(row=6,column=0,columnspan=2,sticky='nsew',pady=10); self.refresh_messages(tree)

    def schedule_message(self,phone,when,channel,msg,parent):
        try:datetime.strptime(when,'%Y-%m-%d %H:%M')
        except:return messagebox.showerror('خطأ','صيغة التاريخ غير صحيحة',parent=parent)
        if not phone or not msg:return messagebox.showerror('خطأ','رقم الهاتف ونص الرسالة مطلوبان',parent=parent)
        self.db.execute('INSERT INTO messages(phone,message,send_at,channel) VALUES(?,?,?,?)',(phone,msg,when,channel)); self.db.commit(); self.backup(); messagebox.showinfo('تم','تمت جدولة الرسالة',parent=parent)
        for child in parent.winfo_children():
            pass

    def refresh_messages(self,tree):
        for x in tree.get_children():tree.delete(x)
        for r in self.db.execute("SELECT phone,send_at,channel,status FROM messages ORDER BY send_at DESC LIMIT 50"):tree.insert('', 'end', values=r)

    def scheduler(self):
        while True:
            try:
                now=datetime.now().strftime('%Y-%m-%d %H:%M'); rows=self.db.execute("SELECT id,phone,message,channel FROM messages WHERE status='pending' AND send_at<=?",(now,)).fetchall()
                for mid,phone,msg,ch in rows:
                    n=''.join(x for x in phone if x.isdigit())
                    url=('https://wa.me/'+n+'?text='+urllib.parse.quote(msg)) if ch=='WhatsApp' else ('sms:'+phone+'?body='+urllib.parse.quote(msg))
                    webbrowser.open(url); self.db.execute("UPDATE messages SET status='opened' WHERE id=?",(mid,)); self.db.commit()
            except Exception:pass
            time.sleep(30)

    def open_print(self):
        top=self.win('طباعة الفواتير',820,500)
        f=tk.Frame(top,bg='#f5f7fb',padx=25,pady=20); f.pack(fill='both',expand=True)
        tk.Label(f,text='أنشئ الفاتورة أو التقرير بصيغة PDF ثم اطبعها عبر الطابعة الافتراضية في Windows.',bg='#f5f7fb',fg='#334155',font=('Segoe UI',13),wraplength=700).pack(pady=18)
        ttk.Button(f,text='إنشاء PDF للطباعة',command=lambda:self.export_pdf('','طباعة',top)).pack(pady=8)
        ttk.Button(f,text='فتح إعدادات الطابعة',command=lambda:os.system('control printers')).pack(pady=8)
        tk.Label(f,text='يدعم الطابعات المتصلة بالكمبيوتر التي يتعرف عليها Windows.',bg='#f5f7fb',fg='#64748b').pack(pady=20)

if __name__ == '__main__':
    root=tk.Tk(); app=MedicalAccountingApp(root); root.mainloop()
