program Test2;

type
  IPrintable = interface
    procedure Print();
  end;

  TBase = class
  public
    procedure Print();
    begin
      writeln(1);
    end;
  end;

  TChild = class(TBase, IPrintable)
  public
    procedure Print();
    begin
      writeln(2);
    end;
  end;

var
  base: TBase;
  child: TChild;

begin
  base := TBase.Create();
  base.Print();

  child := TChild.Create();
  child.Print();
end.
