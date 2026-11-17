import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { server } from "../../test/server";
import { ChannelsPage } from "./ChannelsPage";

vi.mock("../auth/useAuth", () => ({ useAuth: () => ({ user: { id: 2, role: "EMPLOYEE", jobTitle: "SOFTWARE_DEVELOPER" } }) }));
afterEach(() => vi.unstubAllGlobals());

describe("ChannelsPage", () => {
  it("joins a public channel and sends a message", async () => {
    class Socket { onopen: (() => void) | null = null; onmessage: (() => void) | null = null; onclose: (() => void) | null = null; onerror: (() => void) | null = null; close() {} }
    vi.stubGlobal("WebSocket", Socket);
    let member = false; let content = "";
    server.use(
      http.get("*/api/v1/channels", () => HttpResponse.json([{ id: 10, name: "Genel", description: null, visibility: "PUBLIC", ownerId: 1, member, createdAt: "2026-11-17T10:00:00Z" }])),
      http.post("*/api/v1/channels/10/join", () => { member = true; return new HttpResponse(null, { status: 204 }); }),
      http.get("*/api/v1/channels/10/messages", () => HttpResponse.json([
        { id: 19, channelId: 10, authorId: 1, authorName: "Ali", eventType: "MEMBER_REMOVED", subjectName: "Deniz", body: null, createdAt: "2026-11-17T09:00:00Z", editedAt: null, deleted: false },
        ...(content ? [{ id: 20, channelId: 10, authorId: 2, authorName: "Ada", displayName: null, avatarId: null, hasPhoto: false, body: content, createdAt: "2026-11-17T10:00:00Z", editedAt: null, deleted: false }] : []),
      ])),
      http.post("*/api/v1/channels/10/messages", async ({ request }) => { content = (await request.json() as { body:string }).body; return HttpResponse.json({ id: 20 }, { status: 201 }); }),
    );
    const user = userEvent.setup();
    render(<MemoryRouter><ChannelsPage /></MemoryRouter>);
    await user.click(await screen.findByRole("button", { name: /Genel/ }));
    await user.click(screen.getByRole("button", { name: "Kanala katıl" }));
    expect(await screen.findByText("Deniz, Ali tarafından kanaldan çıkarıldı.")).toBeInTheDocument();
    await user.type(await screen.findByLabelText("Mesaj"), "Merhaba ekip");
    await user.click(screen.getByRole("button", { name: "Gönder" }));
    await waitFor(() => expect(screen.getByText("Merhaba ekip")).toBeInTheDocument());
    expect(content).toBe("Merhaba ekip");
  });
});
