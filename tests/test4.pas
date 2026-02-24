program Test4;

type
  TSecret = class
  private
    secret: integer;
  public
    constructor Create();
    begin
      secret := 7;
    end;
  end;

var
  s: TSecret;

begin
  s := TSecret.Create();
  writeln(s.secret);
end.
