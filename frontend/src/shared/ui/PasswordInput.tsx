import { useState, type InputHTMLAttributes } from "react";

type Props = Omit<InputHTMLAttributes<HTMLInputElement>, "type">;

export function PasswordInput(props: Props) {
  const [visible, setVisible] = useState(false);
  return <div className="password-input">
    <input {...props} type={visible ? "text" : "password"} />
    <button type="button" aria-pressed={visible} aria-label={visible ? "Parolayı gizle" : "Parolayı göster"} onClick={() => setVisible((value) => !value)}>
      {visible ? "Gizle" : "Göster"}
    </button>
  </div>;
}
