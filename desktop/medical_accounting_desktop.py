import json, os, sqlite3, threading, time, webbrowser, urllib.parse
from datetime import datetime
import tkinter as tk
from tkinter import ttk, messagebox, filedialog

APP_DIR = os.path.join(os.environ.get('APPDATA', os.path.expanduser('~')), 'MedicalAccounting')
os.makedirs(APP_DIR, exist_ok=True)
DB = os.path.join(APP_DIR, 'medical_accounting.db')
BACKUP_DIR = os.path.join(APP_DIR, 'backups'); os.makedirs(BACKUP_DIR, exist_ok=True)

SCHEMA = '''
CREATE TABLE IF NOT EXISTS patients(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT,case_type TEXT,diagnosis TEXT,notes TEXT,created_at TEXT);
CREATE TABLE IF NOT EXISTS transactions(id INTEGER PRIMARY KEY AUTOINCREMENT,patient_id INTEGER,section TEXT,description TEXT,amount REAL DEFAULT 0,paid REAL DEFAULT 0,remaining REAL DEFAULT 0,doctor_share REAL DEFAULT 0,lab_share REAL DEFAULT 0,created_at TEXT,FOREIGN KEY(patient_id) REFERENCES patients(id));
CREATE TABLE IF NOT EXISTS expenses(id INTEGER PRIMARY KEY AUTOINCREMENT,category TEXT,description TEXT,amount REAL,created_at TEXT);
CREATE TABLE IF NOT EXISTS messages(id INTEGER PRIMARY KEY AUTOINCREMENT,patient_id INTEGER,phone TEXT,message TEXT,send_at TEXT,channel TEXT,status TEXT DEFAULT 'pending');
'''

class App:
    def __init__(self, root):
        self.root=root; self.root.title('المحاسب الطبي الشامل - Windows'); self.root.geometry('1200x760'); self.root.minsize(1000,650)
        self.db=sqlite3.connect(DB, check_same_thread=False); self.db.executescript(SCHEMA); self.db.commit()
        self.build(); self.refresh(); self.backup(); threading.Thread(target=self.scheduler,daemon=True).start()

    def build(self):
        top=ttk.Frame(self.root,padding=12); top.pack(fill='x')
        ttk.Label(top,text='التطبيق المحاسبي الطبي الشامل',font=('Segoe UI',22,'bold')).pack(side='right')
        ttk.Label(top,text='Windows Desktop',font=('Segoe UI',12)).pack(side='left')
        self.tabs=ttk.Notebook(self.root); self.tabs.pack(fill='both',expand=True,padx=10,pady=5)
        self.patient_tab(); self.doctor_tab(); self.lab_tab(); self.pharmacy_tab(); self.expense_tab(); self.search_tab(); self.reports_tab(); self.msg_tab(); self.backup_tab()

    def patient_tab(self):
        f=ttk.Frame(self.tabs,padding=12); self.tabs.add(f,text='الاستقبال')
        self.vars={k:tk.StringVar() for k in ['name','phone','case','diagnosis','notes','amount','paid']}
        fields=[('name','اسم المريض'),('phone','الهاتف'),('case','نوع الحالة'),('diagnosis','التشخيص'),('notes','ملاحظات'),('amount','إجمالي المعالجة'),('paid','الواصل')]
        for i,(k,l) in enumerate(fields): ttk.Label(f,text=l).grid(row=i,column=0,padx=8,pady=7,sticky='e'); ttk.Entry(f,textvariable=self.vars[k],width=60).grid(row=i,column=1,padx=8,pady=7,sticky='ew')
        ttk.Button(f,text='حفظ المريض والعملية',command=self.save_patient).grid(row=len(fields),column=1,pady=15,sticky='e')
        f.columnconfigure(1,weight=1)

    def section_tab(self,title,section):
        f=ttk.Frame(self.tabs,padding=12); self.tabs.add(f,text=title)
        name=tk.StringVar(); desc=tk.StringVar(); amount=tk.StringVar(); paid=tk.StringVar()
        for r,(lab,var) in enumerate([('اسم المريض',name),('الوصف / الخدمة',desc),('المبلغ',amount),('الواصل',paid)]):
            ttk.Label(f,text=lab).grid(row=r,column=0,pady=8,sticky='e'); ttk.Entry(f,textvariable=var,width=60).grid(row=r,column=1,pady=8,sticky='ew')
        def save():
            row=self.db.execute('SELECT id FROM patients WHERE name=? ORDER BY id DESC LIMIT 1',(name.get().strip(),)).fetchone()
            if not row: messagebox.showerror('خطأ','سجّل المريض أولاً من الاستقبال أو اكتب اسماً موجوداً.'); return
            try: a=float(amount.get() or 0); p=float(paid.get() or 0)
            except: messagebox.showerror('خطأ','المبلغ والواصل يجب أن يكونا أرقاماً.'); return
            self.db.execute('INSERT INTO transactions(patient_id,section,description,amount,paid,remaining,created_at) VALUES(?,?,?,?,?,?,?)',(row[0],section,desc.get(),a,p,a-p,datetime.now().isoformat(timespec='seconds'))); self.db.commit(); self.backup(); messagebox.showinfo('تم','تم حفظ العملية.')
        ttk.Button(f,text='حفظ العملية',command=save).grid(row=4,column=1,pady=15,sticky='e'); f.columnconfigure(1,weight=1)
        return f
    def doctor_tab(self): self.section_tab('الطبيب','الطبيب')
    def lab_tab(self): self.section_tab('المختبرات','المختبرات')
    def pharmacy_tab(self): self.section_tab('الصيدلية','الصيدلية')
    def expense_tab(self):
        f=ttk.Frame(self.tabs,padding=12); self.tabs.add(f,text='الخرجيات')
        c,d,a=[tk.StringVar() for _ in range(3)]
        for r,(lab,var) in enumerate([('نوع الخرجية',c),('الوصف',d),('المبلغ',a)]): ttk.Label(f,text=lab).grid(row=r,column=0,pady=8,sticky='e'); ttk.Entry(f,textvariable=var,width=60).grid(row=r,column=1,pady=8)
        def save():
            try:x=float(a.get() or 0)
            except: messagebox.showerror('خطأ','المبلغ غير صحيح'); return
            self.db.execute('INSERT INTO expenses(category,description,amount,created_at) VALUES(?,?,?,?)',(c.get(),d.get(),x,datetime.now().isoformat(timespec='seconds'))); self.db.commit(); self.backup(); messagebox.showinfo('تم','تم تسجيل الخرجية.')
        ttk.Button(f,text='حفظ الخرجية',command=save).grid(row=3,column=1,sticky='e')

    def save_patient(self):
        v=self.vars
        try:a=float(v['amount'].get() or 0); p=float(v['paid'].get() or 0)
        except: messagebox.showerror('خطأ','المبالغ يجب أن تكون أرقاماً.'); return
        if not v['name'].get().strip(): messagebox.showerror('خطأ','اسم المريض مطلوب.'); return
        cur=self.db.execute('INSERT INTO patients(name,phone,case_type,diagnosis,notes,created_at) VALUES(?,?,?,?,?,?)',(v['name'].get(),v['phone'].get(),v['case'].get(),v['diagnosis'].get(),v['notes'].get(),datetime.now().isoformat(timespec='seconds'))); pid=cur.lastrowid
        self.db.execute('INSERT INTO transactions(patient_id,section,description,amount,paid,remaining,created_at) VALUES(?,?,?,?,?,?,?)',(pid,'الاستقبال','معاينة / معالجة',a,p,a-p,datetime.now().isoformat(timespec='seconds'))); self.db.commit(); self.backup(); self.refresh(); messagebox.showinfo('تم','تم حفظ المريض والعملية.')

    def search_tab(self):
        f=ttk.Frame(self.tabs,padding=10); self.tabs.add(f,text='البحث السريع')
        q=tk.StringVar(); ttk.Entry(f,textvariable=q,width=70).pack(side='right',padx=5); ttk.Button(f,text='بحث',command=lambda:self.do_search(q.get())).pack(side='right')
        self.search_tree=ttk.Treeview(f,columns=('id','name','phone','case','diag','section','amount','paid','remain','date'),show='headings')
        for c,t in zip(self.search_tree['columns'],['ID','المريض','الهاتف','الحالة','التشخيص','القسم','المبلغ','الواصل','الباقي','التاريخ']): self.search_tree.heading(c,text=t)
        self.search_tree.pack(fill='both',expand=True,pady=10)
    def do_search(self,q):
        for x in self.search_tree.get_children(): self.search_tree.delete(x)
        like='%'+q+'%'; rows=self.db.execute('SELECT p.id,p.name,p.phone,p.case_type,p.diagnosis,t.section,t.amount,t.paid,t.remaining,t.created_at FROM patients p LEFT JOIN transactions t ON p.id=t.patient_id WHERE p.name LIKE ? OR p.phone LIKE ? OR p.case_type LIKE ? OR p.diagnosis LIKE ? ORDER BY p.id DESC',(like,like,like,like)).fetchall()
        for r in rows:self.search_tree.insert('', 'end', values=r)

    def reports_tab(self):
        f=ttk.Frame(self.tabs,padding=12); self.tabs.add(f,text='الفواتير والتقارير')
        ttk.Label(f,text='اسم المريض (اتركه فارغاً للتقرير العام)').pack(anchor='e'); name=tk.StringVar(); ttk.Entry(f,textvariable=name,width=55).pack(anchor='e',pady=6)
        period=tk.StringVar(value='اليومي'); ttk.Combobox(f,textvariable=period,values=['اليومي','الأسبوعي','الشهري','السنوي'],state='readonly',width=20).pack(anchor='e',pady=6)
        ttk.Button(f,text='إنشاء PDF',command=lambda:self.export_pdf(name.get(),period.get())).pack(anchor='e',pady=5); ttk.Button(f,text='إنشاء Excel',command=lambda:self.export_xlsx(name.get(),period.get())).pack(anchor='e',pady=5); ttk.Button(f,text='طباعة آخر PDF',command=self.print_last).pack(anchor='e',pady=5)
        self.last_pdf=None

    def get_rows(self,name=''):
        if name.strip(): return self.db.execute('SELECT p.name,p.phone,t.section,t.description,t.amount,t.paid,t.remaining,t.created_at FROM patients p JOIN transactions t ON p.id=t.patient_id WHERE p.name LIKE ? ORDER BY t.id DESC',('%'+name.strip()+'%',)).fetchall()
        return self.db.execute('SELECT p.name,p.phone,t.section,t.description,t.amount,t.paid,t.remaining,t.created_at FROM patients p JOIN transactions t ON p.id=t.patient_id ORDER BY t.id DESC').fetchall()
    def export_pdf(self,name,period):
        try:
            from reportlab.lib.pagesizes import A4; from reportlab.pdfgen import canvas
            from reportlab.pdfbase.ttfonts import TTFont; from reportlab.pdfbase import pdfmetrics
            path=filedialog.asksaveasfilename(defaultextension='.pdf',filetypes=[('PDF','*.pdf')],initialfile='تقرير_طبي.pdf');
            if not path:return
            c=canvas.Canvas(path,pagesize=A4); w,h=A4; c.setFont('Helvetica',12); y=h-45
            c.drawString(45,y,'Comprehensive Medical Accounting Report'); y-=25; c.drawString(45,y,'Period: '+period+'   Patient: '+(name or 'All')); y-=30
            for r in self.get_rows(name):
                line=f'{r[0]} | {r[2]} | {r[3]} | Amount {r[4]:.2f} | Paid {r[5]:.2f} | Due {r[6]:.2f}'
                c.drawString(35,y,line[:125]); y-=17
                if y<45:c.showPage(); c.setFont('Helvetica',10); y=h-45
            c.save(); self.last_pdf=path; messagebox.showinfo('تم','تم إنشاء PDF: '+path)
        except Exception as e: messagebox.showerror('PDF',str(e))
    def export_xlsx(self,name,period):
        try:
            from openpyxl import Workbook
            path=filedialog.asksaveasfilename(defaultextension='.xlsx',filetypes=[('Excel','*.xlsx')],initialfile='تقرير_طبي.xlsx');
            if not path:return
            wb=Workbook(); ws=wb.active; ws.title='Medical Report'; ws.append(['Patient','Phone','Section','Description','Amount','Paid','Remaining','Date'])
            for r in self.get_rows(name):ws.append(list(r))
            wb.save(path); messagebox.showinfo('تم','تم إنشاء Excel: '+path)
        except Exception as e: messagebox.showerror('Excel',str(e))
    def print_last(self):
        if not self.last_pdf or not os.path.exists(self.last_pdf): messagebox.showwarning('طباعة','أنشئ PDF أولاً.'); return
        try: os.startfile(self.last_pdf,'print')
        except Exception as e: messagebox.showerror('طباعة',str(e))

    def msg_tab(self):
        f=ttk.Frame(self.tabs,padding=12); self.tabs.add(f,text='الرسائل التلقائية')
        phone=tk.StringVar(); when=tk.StringVar(value=datetime.now().strftime('%Y-%m-%d %H:%M')); channel=tk.StringVar(value='WhatsApp'); text=tk.Text(f,height=10,width=80)
        for r,(lab,var) in enumerate([('رقم الهاتف',phone),('تاريخ ووقت الإرسال YYYY-MM-DD HH:MM',when)]): ttk.Label(f,text=lab).grid(row=r,column=0,pady=6,sticky='e'); ttk.Entry(f,textvariable=var,width=55).grid(row=r,column=1,pady=6)
        ttk.Combobox(f,textvariable=channel,values=['WhatsApp','SMS'],state='readonly',width=18).grid(row=2,column=1,sticky='e'); text.grid(row=3,column=0,columnspan=2,pady=8)
        def add():
            self.db.execute('INSERT INTO messages(phone,message,send_at,channel) VALUES(?,?,?,?)',(phone.get(),text.get('1.0','end').strip(),when.get(),channel.get())); self.db.commit(); self.backup(); messagebox.showinfo('تم','تمت إضافة الرسالة للجدولة.')
        ttk.Button(f,text='جدولة الرسالة',command=add).grid(row=4,column=1,sticky='e')
        ttk.Label(f,text='ملاحظة: Windows يفتح قناة WhatsApp/SMS في وقت الجدولة؛ الإرسال النهائي يعتمد على التطبيق/المشغل ولا يتم تجاوز صلاحياته.').grid(row=5,column=0,columnspan=2,pady=15)
    def scheduler(self):
        while True:
            try:
                now=datetime.now().strftime('%Y-%m-%d %H:%M'); rows=self.db.execute("SELECT id,phone,message,channel FROM messages WHERE status='pending' AND send_at<=?",(now,)).fetchall()
                for mid,phone,msg,ch in rows:
                    if ch=='WhatsApp':
                        n=''.join(x for x in phone if x.isdigit()); url='https://wa.me/'+n+'?text='+urllib.parse.quote(msg)
                    else: url='sms:'+phone+'?body='+urllib.parse.quote(msg)
                    webbrowser.open(url); self.db.execute("UPDATE messages SET status='opened' WHERE id=?",(mid,)); self.db.commit()
            except: pass
            time.sleep(30)
    def backup(self):
        try:
            ts=datetime.now().strftime('%Y%m%d_%H%M%S'); data={'patients':[dict(zip(['id','name','phone','case_type','diagnosis','notes','created_at'],r)) for r in self.db.execute('SELECT * FROM patients')], 'transactions':[dict(zip(['id','patient_id','section','description','amount','paid','remaining','doctor_share','lab_share','created_at'],r)) for r in self.db.execute('SELECT * FROM transactions')], 'expenses':[dict(zip(['id','category','description','amount','created_at'],r)) for r in self.db.execute('SELECT * FROM expenses')]}
            with open(os.path.join(BACKUP_DIR,'backup_'+ts+'.json'),'w',encoding='utf-8') as f: json.dump(data,f,ensure_ascii=False,indent=2)
        except: pass
    def backup_tab(self):
        f=ttk.Frame(self.tabs,padding=12); self.tabs.add(f,text='الحفظ والنسخ الاحتياطية')
        ttk.Label(f,text='يتم الحفظ المحلي تلقائياً بعد العمليات. يمكنك أيضاً أخذ نسخة يدوية.').pack(anchor='e',pady=10)
        ttk.Button(f,text='نسخة JSON يدوية',command=self.manual_backup).pack(anchor='e',pady=5); ttk.Button(f,text='فتح مجلد النسخ',command=lambda:os.startfile(BACKUP_DIR)).pack(anchor='e',pady=5)
    def manual_backup(self):
        path=filedialog.asksaveasfilename(defaultextension='.json',filetypes=[('JSON','*.json')],initialfile='medical_backup.json');
        if not path:return
        src=sorted([os.path.join(BACKUP_DIR,x) for x in os.listdir(BACKUP_DIR) if x.endswith('.json')])[-1]
        with open(src,'rb') as a,open(path,'wb') as b:b.write(a.read())
        messagebox.showinfo('تم','تم حفظ النسخة.')
    def refresh(self): pass

if __name__=='__main__':
    root=tk.Tk(); App(root); root.mainloop()
