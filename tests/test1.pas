program Test1;

type
  TCounter = class
  private
    value: integer;
  public
    constructor Create(v: integer);
    begin
      value := v;
    end;

    procedure Inc;
    begin
      value := value + 1;
    end;

    procedure Print;
    begin
      writeln(value);
    end;
  end;

var
  c: TCounter;

begin
  c := TCounter.Create(5);
  c.Inc();
  c.Print();
end.
